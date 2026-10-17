package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import javax.crypto.KeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoiceAttachmentManifestTest {
    private fun newKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun metadataRoundTripsOnlyAfterEncryptedPayloadIsCommitted() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-voice-manifest").toFile()
        val store = VoiceAttachmentStore(context, { newKey() }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        val attachment = store.save(41L, "recording_1", ByteArrayInputStream(ByteArray(3_200)))

        val expected = VoiceAttachmentMetadata(
            noteId = 41L,
            attachmentId = "recording_1",
            createdAtMillis = 1234L,
            durationMillis = 100L,
            encryptedSizeBytes = attachment.encryptedSizeBytes,
            displayName = "Voice recording"
        )
        manifest.put(expected, store)

        assertEquals(listOf(expected), manifest.list(41L, store))
    }

    @Test
    fun reconciliationRemovesStaleIndexButPreservesEncryptedAudio() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-voice-reconcile").toFile()
        val key = newKey()
        val store = VoiceAttachmentStore(context, { key }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        val attachment = store.save(42L, "keep_audio", ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        manifest.put(
            VoiceAttachmentMetadata(
                noteId = 42L,
                attachmentId = "keep_audio",
                createdAtMillis = 100L,
                durationMillis = 1L,
                encryptedSizeBytes = attachment.encryptedSizeBytes,
                displayName = "Voice recording"
            ),
            store
        )

        assertEquals(1, manifest.reconcile(store, emptySet()))
        assertTrue(File(File(root, "42"), "keep_audio.nva").isFile)
        assertTrue(manifest.list(42L, store).isEmpty())
    }
}
