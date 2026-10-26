package com.nestnote.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nestnote.app.voicenote.VoiceAudioImportPolicy
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Opt-in importer for recordings created by the user's dialer.
 *
 * The source is a user-granted SAF tree. Source files are never modified or deleted.
 * Imported bytes are streamed straight into VoiceAttachmentStore, which encrypts them
 * with its Android Keystore key and does not stage plaintext on disk.
 */
internal object CallRecordingImport {
    const val PREFS = "call_recording_import"
    const val KEY_ENABLED = "enabled"
    const val KEY_TREE_URI = "source_tree_uri"
    const val KEY_STATUS = "last_status"
    const val KEY_STATUS_AT = "last_status_at"
    const val KEY_ITEMS = "items"
    const val STATUS_DISABLED = "Disabled"
    const STATUS_NEEDS_FOLDER = "Choose a recordings folder"
    const val STATUS_READY = "Waiting to import"
    const val STATUS_IMPORTED = "Import complete"
    const val STATUS_SOURCE_UNAVAILABLE = "Source folder unavailable — reconnect it"
    const val STATUS_FAILED = "Import failed — retry available"
    const val RECORDINGS_NOTE_ID = 9_223_372_036_854_770_000L

    data class Item(
        val attachmentId: String,
        val displayName: String,
        val importedAtMillis: Long,
        val durationMillis: Long,
        val sourceUri: String
    )

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun sourceTree(context: Context): Uri? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TREE_URI, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

    fun items(context: Context): List<Item> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return runCatching {
            val array = JSONArray(prefs.getString(KEY_ITEMS, "[]") ?: "[]")
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(Item(
                        attachmentId = item.getString("id"),
                        displayName = item.getString("name"),
                        importedAtMillis = item.getLong("importedAt"),
                        durationMillis = item.optLong("duration", 0L),
                        sourceUri = item.getString("source")
                    ))
                }
            }.sortedByDescending { it.importedAtMillis }
        }.getOrDefault(emptyList())
    }

    fun setStatus(context: Context, status: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATUS, status)
            .putLong(KEY_STATUS_AT, System.currentTimeMillis())
            .apply()
    }

    fun status(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STATUS, if (enabled(context)) STATUS_READY else STATUS_DISABLED)
            ?: STATUS_READY

    fun persistTreeGrant(context: Context, treeUri: Uri): Boolean = runCatching {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(treeUri, flags)
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TREE_URI, treeUri.toString()).commit()
        saved
    }.getOrDefault(false)

    /** Round-trip a small encrypted probe before enabling background imports. */
    fun verifyEncryptionReady(context: Context): Boolean = runCatching {
        val store = VoiceAttachmentStore(context)
        val id = UUID.randomUUID().toString().replace("-", "")
        val probe = "NESTNOTE-CALL-IMPORT".toByteArray(Charsets.UTF_8)
        store.save(RECORDINGS_NOTE_ID, id, ByteArrayInputStream(probe))
        val recovered = store.read(RECORDINGS_NOTE_ID, id)
        val verified = MessageDigest.isEqual(probe, recovered)
        recovered.fill(0)
        probe.fill(0)
        check(verified) { "Encrypted storage verification failed" }
        check(store.delete(RECORDINGS_NOTE_ID, id)) { "Encrypted verification cleanup failed" }
        true
    }.getOrDefault(false)

    fun schedule(context: Context) {
        val manager = WorkManager.getInstance(context)
        val periodic = PeriodicWorkRequestBuilder<CallRecordingImportWorker>(6, TimeUnit.HOURS).build()
        manager.enqueueUniquePeriodicWork(
            "call_recording_import_periodic",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
        val request = OneTimeWorkRequestBuilder<CallRecordingImportWorker>().build()
        manager.enqueueUniqueWork("call_recording_import_scan", ExistingWorkPolicy.KEEP, request)
    }

    fun removeImportedCopy(context: Context, attachmentId: String): Boolean {
        val store = VoiceAttachmentStore(context)
        val removed = runCatching { store.delete(RECORDINGS_NOTE_ID, attachmentId) }.getOrDefault(false)
        if (removed) {
            val remaining = items(context).filterNot { it.attachmentId == attachmentId }
            saveItems(context, remaining)
        }
        return removed
    }

    internal fun saveItems(context: Context, items: List<Item>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.attachmentId)
                put("name", item.displayName)
                put("importedAt", item.importedAtMillis)
                put("duration", item.durationMillis)
                put("source", item.sourceUri)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ITEMS, array.toString()).commit()
    }
}

internal class CallRecordingImportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!CallRecordingImport.enabled(applicationContext)) return@withContext Result.success()
        val treeUri = CallRecordingImport.sourceTree(applicationContext)
            ?: return@withContext fail(CallRecordingImport.STATUS_NEEDS_FOLDER)
        val resolver = applicationContext.contentResolver
        val root = runCatching { DocumentFile.fromTreeUri(applicationContext, treeUri) }.getOrNull()
        if (root == null || !root.isDirectory || !root.canRead()) {
            return@withContext fail(CallRecordingImport.STATUS_SOURCE_UNAVAILABLE)
        }

        val priorItems = CallRecordingImport.items(applicationContext).toMutableList()
        val importedSources = priorItems.mapTo(mutableSetOf()) { it.sourceUri }
        val store = runCatching { VoiceAttachmentStore(applicationContext) }.getOrElse {
            return@withContext fail(CallRecordingImport.STATUS_FAILED)
        }
        var importedCount = 0
        var oversizedFileFound = false
        return@withContext try {
            root.listFiles().filter { it.isFile && it.canRead() }.forEach { file ->
                if (!CallRecordingImport.enabled(applicationContext)) return@forEach
                val uri = file.uri
                val sourceKey = uri.toString()
                if (sourceKey in importedSources) return@forEach
                val name = file.name ?: queryName(resolver, uri) ?: "Call recording"
                val mime = resolver.getType(uri)?.lowercase(java.util.Locale.ROOT)
                if (!VoiceAudioImportPolicy.isAudioDocument(name, mime)) return@forEach
                val declared = file.length()
                if (declared > com.nestnote.app.voicenote.VoiceNoteRepository.MAX_IMPORT_BYTES) {
                    // Keep the source untouched and surface an actionable error.
                    oversizedFileFound = true
                    return@forEach
                }
                val id = UUID.randomUUID().toString().replace("-", "")
                val input = resolver.openInputStream(uri) ?: throw IOException("Source recording is no longer accessible")
                input.use { store.save(CallRecordingImport.RECORDINGS_NOTE_ID, id, it) }
                priorItems += CallRecordingImport.Item(
                    attachmentId = id,
                    displayName = name.take(160),
                    importedAtMillis = System.currentTimeMillis(),
                    durationMillis = readDuration(resolver, uri),
                    sourceUri = sourceKey
                )
                importedSources += sourceKey
                importedCount++
                CallRecordingImport.saveItems(applicationContext, priorItems)
            }
            CallRecordingImport.setStatus(
                applicationContext,
                when {
                    oversizedFileFound -> "A source recording exceeds the 100 MB import limit"
                    importedCount > 0 -> CallRecordingImport.STATUS_IMPORTED
                    else -> CallRecordingImport.STATUS_READY
                }
            )
            Result.success()
        } catch (_: SecurityException) {
            fail(CallRecordingImport.STATUS_SOURCE_UNAVAILABLE)
        } catch (_: Exception) {
            fail(CallRecordingImport.STATUS_FAILED)
        }
    }

    private fun queryName(resolver: android.content.ContentResolver, uri: Uri): String? =
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()

    private fun readDuration(resolver: android.content.ContentResolver, uri: Uri): Long {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(applicationContext, uri)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun fail(status: String): Result {
        CallRecordingImport.setStatus(applicationContext, status)
        return Result.retry()
    }
}
