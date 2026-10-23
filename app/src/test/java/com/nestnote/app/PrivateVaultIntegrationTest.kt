package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrivateVaultIntegrationTest {
    private lateinit var context: Context
    private lateinit var generalStore: NoteStore
    private lateinit var privateStore: PrivateNoteStore
    private lateinit var crypto: PrivateNoteCrypto

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        listOf("nestnote_local", "nestnote_private_vault_crypto", "nestnote_private_note_records")
            .forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        val generalKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        generalStore = NoteStore(context) { generalKey }
        privateStore = PrivateNoteStore(context)
        crypto = PrivateNoteCrypto(context)
    }

    @Test fun migrationMovesPrivateRecordsOutOfGeneralStoreOnlyAfterEncryptedCommit() {
        val public = NestNote(id = 11L, title = "Public", body = "ordinary")
        val private = NestNote(id = 12L, title = "Private marker", body = "Secret marker", privateNote = true)
        assertTrue(generalStore.save(listOf(public, private)))

        val legacy = generalStore.loadSafely().notes.filter { it.privateNote }
        assertEquals(listOf(private), legacy)
        assertTrue(crypto.initialize("vault password".toCharArray()))
        val session = crypto.unlock("vault password".toCharArray())!!
        assertTrue(privateStore.save(session, legacy))
        assertTrue(generalStore.save(listOf(public)))

        assertEquals(listOf(public), generalStore.loadSafely().notes)
        assertEquals(listOf(private), privateStore.load(session))
        val privateDisk = context.getSharedPreferences("nestnote_private_note_records", Context.MODE_PRIVATE)
            .getString("encrypted_records", "")!!
        assertFalse(privateDisk.contains(private.title))
        assertFalse(privateDisk.contains(private.body))
    }

    @Test fun failedVaultUnlockDoesNotChangeGeneralStoreOrRevealPrivateRecords() {
        val public = NestNote(id = 21L, title = "Public", body = "ordinary")
        val private = NestNote(id = 22L, title = "Private", body = "secret", privateNote = true)
        assertTrue(generalStore.save(listOf(public, private)))
        assertTrue(crypto.initialize("right password".toCharArray()))
        assertEquals(null, crypto.unlock("wrong password".toCharArray()))
        assertEquals(listOf(public, private), generalStore.loadSafely().notes)
    }
}
