package com.nestnote.app.voicenote

import android.content.Context
import com.nestnote.app.VoiceAttachment
import com.nestnote.app.VoiceAttachmentManifest
import com.nestnote.app.VoiceAttachmentMetadata
import com.nestnote.app.VoiceAttachmentStore
import java.io.IOException
import java.io.InputStream

/**
 * Voice-note backend boundary for the editor and future voice-note UI.
 *
 * This repository coordinates encrypted payload storage with its durable metadata index.
 * The existing storage implementations remain the source of truth for encryption and atomic
 * file handling; this class keeps callers from coordinating those two components themselves.
 *
 * Payloads are encrypted by VoiceAttachmentStore using its dedicated Android Keystore key.
 * They are deliberately independent of ordinary-note encryption and Private Vault keys.
 */
class VoiceNoteRepository(context: Context) {
    private val store = VoiceAttachmentStore(context)
    private val manifest = VoiceAttachmentManifest(context)

    /** Returns only manifest entries whose encrypted payloads still exist and match the index. */
    @Throws(IOException::class)
    fun list(noteId: Long): List<VoiceAttachmentMetadata> = manifest.list(noteId, store)

    /**
     * Encrypt and persist the recording first, then publish metadata. If metadata publication
     * fails, remove only the newly-created payload so no unindexed partial save is left behind.
     */
    @Throws(IOException::class)
    fun save(
        noteId: Long,
        attachmentId: String,
        input: InputStream,
        createdAtMillis: Long,
        durationMillis: Long,
        displayName: String
    ): VoiceAttachmentMetadata {
        val saved: VoiceAttachment = store.save(noteId, attachmentId, input)
        val metadata = VoiceAttachmentMetadata(
            noteId = noteId,
            attachmentId = saved.attachmentId,
            createdAtMillis = createdAtMillis,
            durationMillis = durationMillis,
            encryptedSizeBytes = saved.encryptedSizeBytes,
            displayName = displayName
        )
        try {
            manifest.put(metadata, store)
            return metadata
        } catch (error: Exception) {
            // Do not leave a payload from a save whose metadata commit failed.
            // If cleanup itself fails, retain the original exception and its context.
            runCatching { store.delete(noteId, saved.attachmentId) }
            throw IOException("Voice recording could not be committed", error)
        }
    }

    /** Authentication is verified by AES-GCM before plaintext bytes are returned. */
    @Throws(IOException::class)
    fun read(noteId: Long, attachmentId: String): ByteArray = store.read(noteId, attachmentId)

    /**
     * Delete ciphertext first; a failed payload deletion must not hide the recording from the
     * metadata index. A stale metadata entry is safe and can be reconciled on the next list.
     */
    @Throws(IOException::class)
    fun delete(noteId: Long, attachmentId: String) {
        if (!store.delete(noteId, attachmentId)) {
            throw IOException("Voice recording could not be deleted")
        }
        manifest.remove(noteId, attachmentId)
    }

    /** Removes stale index rows only. It intentionally does not delete unindexed audio files. */
    @Throws(IOException::class)
    fun reconcile(liveNoteIds: Set<Long>): Int = manifest.reconcile(store, liveNoteIds)

    /** Use only after the owning note has been durably deleted or a new draft is discarded. */
    @Throws(IOException::class)
    fun deleteAllForNote(noteId: Long) {
        if (!store.deleteAll(noteId)) throw IOException("Voice recordings could not be deleted")
        manifest.removeNote(noteId)
    }

    companion object {
        const val MAX_RECORDING_BYTES: Int = VoiceAttachmentStore.MAX_AUDIO_BYTES
    }
}
