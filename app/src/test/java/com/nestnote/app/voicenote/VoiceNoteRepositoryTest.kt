package com.nestnote.app.voicenote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nestnote.app.VoiceAttachmentManifest
import com.nestnote.app.VoiceAttachmentStore
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import javax.crypto.KeyGenerator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoiceNoteRepositoryTest {
    private fun newKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private fun repository(root: File, key: javax.crypto.SecretKey = newKey()): VoiceNoteRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return VoiceNoteRepository(
            VoiceAttachmentStore(context, { key }, root),
            VoiceAttachmentManifest(context, root)
        )
    }

    @Test
    fun saveListsAndReadsOnlyCommittedEncryptedAttachment() {
        val root = Files.createTempDirectory("nestnote-voice-repository").toFile()
        val repository = repository(root)
        val plaintext = "encrypted voice payload".toByteArray()

        val metadata = repository.save(
            noteId = 51L,
            attachmentId = "clip_51",
            input = ByteArrayInputStream(plaintext),
            createdAtMillis = 123L,
            durationMillis = 456L,
            displayName = "Meeting note"
        )

        assertEquals(listOf(metadata), repository.list(51L))
        assertEquals(plaintext.toList(), repository.read(51L, "clip_51").toList())
        assertTrue(File(File(root, "51"), "clip_51.nva").isFile)
        assertFalse(
            File(File(root, "51"), "clip_51.nva").readBytes()
                .toString(Charsets.ISO_8859_1).contains("encrypted voice payload")
        )
    }

    @Test
    fun metadataCommitFailureRemovesOnlyNewPayload() {
        val root = Files.createTempDirectory("nestnote-voice-repository-failed-index").toFile()
        File(root, "manifest_v1.json").writeText("{ corrupt manifest")
        val repository = repository(root)

        try {
            repository.save(
                noteId = 52L,
                attachmentId = "must_not_publish",
                input = ByteArrayInputStream(byteArrayOf(1, 2, 3)),
                createdAtMillis = 1L,
                durationMillis = 1L,
                displayName = "Recording"
            )
            throw AssertionError("A corrupt manifest must prevent save")
        } catch (expected: IOException) {
            assertTrue(expected.message!!.contains("could not be committed"))
        }

        assertTrue(repository.listPayloads(52L).isEmpty())
        assertFalse(File(File(root, "52"), "must_not_publish.nva").exists())
    }

    @Test
    fun deleteAllForOneNotePreservesOtherNotes() {
        val root = Files.createTempDirectory("nestnote-voice-repository-delete").toFile()
        val key = newKey()
        val repository = repository(root, key)
        val retainedBytes = byteArrayOf(9, 8, 7)
        repository.save(53L, "remove_me", ByteArrayInputStream(byteArrayOf(1, 2)), 1L, 1L, "Temporary")
        repository.save(54L, "keep_me", ByteArrayInputStream(retainedBytes), 2L, 2L, "Saved")

        repository.deleteAllForNote(53L)

        assertTrue(repository.listPayloads(53L).isEmpty())
        assertTrue(repository.list(53L).isEmpty())
        assertArrayEquals(retainedBytes, repository.read(54L, "keep_me"))
        assertEquals(listOf("keep_me"), repository.list(54L).map { it.attachmentId })
    }

    @Test
    fun orphanPayloadRemainsDiscoverableWhenManifestEntryIsMissing() {
        val root = Files.createTempDirectory("nestnote-voice-repository-orphan").toFile()
        val repository = repository(root)
        repository.save(55L, "indexed", ByteArrayInputStream(byteArrayOf(1, 2)), 1L, 1L, "Indexed")
        val manifestFile = File(root, "manifest_v1.json")
        manifestFile.writeText("""{"schemaVersion":1,"attachments":[]}""")

        assertTrue(repository.list(55L).isEmpty())
        assertEquals(listOf("indexed"), repository.listPayloads(55L).map { it.attachmentId })
        assertTrue(File(File(root, "55"), "indexed.nva").isFile)
    }
}
