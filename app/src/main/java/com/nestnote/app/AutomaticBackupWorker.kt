package com.nestnote.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Periodic local backup. Android may defer this work under Doze, battery restrictions,
 * or storage pressure; it is best-effort rather than a clock-exact schedule.
 *
 * Shares its preference file and key names with the Backup screen (see Constants.kt)
 * so the toggle the user flips in the UI is the same flag this worker observes, and
 * the run-info this worker writes is the same info the UI displays.
 */
class AutomaticBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PREF_BACKUP_AUTOMATIC_ENABLED, false)) return Result.success()

        return try {
            val loaded = NoteStore(applicationContext).loadSafely()
            if (loaded.recoveryRequired) {
                prefs.edit()
                    .putLong(PREF_BACKUP_AUTOMATIC_LAST_RUN, System.currentTimeMillis())
                    .putString(PREF_BACKUP_AUTOMATIC_STATUS, "Failed: notes need recovery")
                    .apply()
                return Result.failure()
            }
            val backupDir = File(applicationContext.filesDir, "automatic-backups")
            if (!backupDir.exists() && !backupDir.mkdirs()) error("Cannot create backup directory")
            val target = File(backupDir, "latest.json")
            val temp = File(backupDir, "latest.json.tmp")
            temp.writeText(NoteStore.encodeBackup(loaded.notes), Charsets.UTF_8)
            // Never delete the last good backup before the replacement is ready. Prefer an
            // atomic same-directory replacement and fall back only when atomic moves are unsupported.
            try {
                Files.move(
                    temp.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            prefs.edit()
                .putLong(PREF_BACKUP_AUTOMATIC_LAST_RUN, System.currentTimeMillis())
                .putString(PREF_BACKUP_AUTOMATIC_STATUS, "Success")
                .putInt(PREF_BACKUP_AUTOMATIC_LAST_COUNT, loaded.notes.size)
                .apply()
            Result.success()
        } catch (_: Exception) {
            prefs.edit()
                .putLong(PREF_BACKUP_AUTOMATIC_LAST_RUN, System.currentTimeMillis())
                .putString(PREF_BACKUP_AUTOMATIC_STATUS, "Failed")
                .apply()
            Result.retry()
        }
    }
}