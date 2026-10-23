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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrivateNoteStoreTest {
    private lateinit var context: Context
    private lateinit var crypto: PrivateNoteCrypto
    private lateinit var store: PrivateNoteStore
    private lateinit var session: PrivateNoteCrypto.Session

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("nestnote_private_vault_crypto", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("nestnote_private_note_records", Context.MODE_PRIVATE).edit().clear().commit()
        crypto = PrivateNoteCrypto(context)
        assertTrue(crypto.initialize("vault password".toCharArray()))
        session = crypto.unlock("vault password".toCharArray())!!
        store = PrivateNoteStore(context)
    }

    @Test fun privateRecordsRoundTripAndDiskDoesNotContainPlaintext() {
        val note = NestNote(id = 1001L, title = "Private title marker", body = "Private body marker", privateNote = true)
        assertTrue(store.save(session, listOf(note)))
        val disk = context.getSharedPreferences("nestnote_private_note_records", Context.MODE_PRIVATE)
            .getString("encrypted_records", "")!!
        assertFalse(disk.contains(note.title))
        assertFalse(disk.contains(note.body))
        assertEquals(listOf(note), store.load(session))
    }

    @Test fun publicNotesCannotBeWrittenIntoPrivateStore() {
        assertFalse(store.save(session, listOf(NestNote(id = 1002L, title = "public", body = "body"))))
    }

    @Test fun wrongSessionCannotReadPrivateRecords() {
        val note = NestNote(id = 1003L, title = "Private", body = "secret", privateNote = true)
        assertTrue(store.save(session, listOf(note)))
        val other = PrivateNoteCrypto(context)
        assertTrue(other.initialize("another password".toCharArray()))
        val wrong = other.unlock("another password".toCharArray())!!
        assertFalse(runCatching { store.load(wrong) }.isSuccess)
    }
}
