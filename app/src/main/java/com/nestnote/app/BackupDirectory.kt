package com.nestnote.app

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException

/**
 * Uses a persisted Storage Access Framework tree grant so exports can be written to
 * Documents/NESTNOTE without showing a picker on every backup.
 *
 * Android intentionally requires a one-time user-granted directory permission. This
 * helper never attempts broad storage permission or direct filesystem access to Documents.
 */
internal object BackupDirectory {
    const val PREFERENCE_KEY = "backup_documents_tree_uri"
    private const val FOLDER_NAME = "NESTNOTE"
    private const val BACKUP_NAME = "NESTNOTE-backup.json"
    private const val MIME_JSON = "application/json"
    private const val ENCRYPTED_BACKUP_NAME = "NESTNOTE-backup.nestnote"
    private const val MIME_ENCRYPTED = "application/octet-stream"

    fun persistTreeGrant(context: Context, treeUri: Uri): Boolean = runCatching {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().putString(PREFERENCE_KEY, treeUri.toString()).commit()
    }.getOrDefault(false)

    fun configuredTreeUri(context: Context): Uri? =
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .getString(PREFERENCE_KEY, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

    fun writeLatest(context: Context, treeUri: Uri, bytes: ByteArray): Boolean = runCatching {
        require(bytes.isNotEmpty() && bytes.size <= NoteStore.MAX_BACKUP_BYTES)
        val folder = nestNoteFolder(context, treeUri, create = true) ?: error("Cannot access Documents/NESTNOTE")
        val existing = folder.findFile(BACKUP_NAME)
        val target = existing ?: folder.createFile(MIME_JSON, BACKUP_NAME) ?: error("Cannot create backup file")
        context.contentResolver.openOutputStream(target.uri, "rwt")?.use { output ->
            output.write(bytes)
            output.flush()
        } ?: error("Cannot open backup destination")
        true
    }.getOrDefault(false)

    fun readLatest(context: Context, treeUri: Uri): ByteArray? = runCatching {
        val folder = nestNoteFolder(context, treeUri, create = false) ?: return null
        val file = folder.findFile(BACKUP_NAME) ?: return null
        val output = java.io.ByteArrayOutputStream()
        context.contentResolver.openInputStream(file.uri)?.use { input ->
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > NoteStore.MAX_BACKUP_BYTES) throw IOException("Backup is too large")
                output.write(buffer, 0, count)
            }
        } ?: return null
        output.toByteArray()
    }.getOrNull()

    fun writeEncryptedLatest(context: Context, treeUri: Uri, bytes: ByteArray): Boolean = runCatching {
        require(bytes.isNotEmpty() && bytes.size <= NoteStore.MAX_BACKUP_BYTES)
        val folder = nestNoteFolder(context, treeUri, create = true) ?: error("Cannot access Documents/NESTNOTE")
        val existing = folder.findFile(ENCRYPTED_BACKUP_NAME)
        val target = existing ?: folder.createFile(MIME_ENCRYPTED, ENCRYPTED_BACKUP_NAME)
            ?: error("Cannot create encrypted backup file")
        context.contentResolver.openOutputStream(target.uri, "wt")?.use { output ->
            output.write(bytes)
            output.flush()
        } ?: error("Cannot open encrypted backup destination")
        true
    }.getOrDefault(false)

    fun readEncryptedLatest(context: Context, treeUri: Uri): ByteArray? = runCatching {
        val folder = nestNoteFolder(context, treeUri, create = false) ?: return null
        val file = folder.findFile(ENCRYPTED_BACKUP_NAME) ?: return null
        val output = java.io.ByteArrayOutputStream()
        context.contentResolver.openInputStream(file.uri)?.use { input ->
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > NoteStore.MAX_BACKUP_BYTES) throw IOException("Encrypted backup is too large")
                output.write(buffer, 0, count)
            }
        } ?: return null
        output.toByteArray()
    }.getOrNull()

    fun encryptedBackupName(): String = ENCRYPTED_BACKUP_NAME

    fun folderName(): String = FOLDER_NAME
    fun backupName(): String = BACKUP_NAME

    private fun nestNoteFolder(context: Context, treeUri: Uri, create: Boolean): DocumentFile? {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val existing = root.findFile(FOLDER_NAME)
        if (existing != null) return existing.takeIf { it.isDirectory }
        if (!create) return null
        return root.createDirectory(FOLDER_NAME)
    }
}
