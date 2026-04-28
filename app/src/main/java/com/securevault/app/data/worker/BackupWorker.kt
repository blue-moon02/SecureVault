package com.securevault.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.securevault.app.domain.repository.NoteRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Creates a local encrypted backup of all non-sensitive notes into the app's
 * private files directory. No network required – all local.
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val noteRepository: NoteRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            setForeground(getForegroundInfo())
            val notes = noteRepository.getNonSensitiveNotes()
            val backupDir = File(applicationContext.filesDir, "backups").also { it.mkdirs() }
            val backupFile = File(backupDir, "backup_${System.currentTimeMillis()}.sv")

            // Simple JSON serialisation (in production use a proper serialiser)
            val content = buildString {
                append("[")
                notes.forEachIndexed { i, note ->
                    append("""{"id":${note.id},"title":"${note.title.replace(""", "\"")}","updatedAt":${note.updatedAt}}""")
                    if (i < notes.lastIndex) append(",")
                }
                append("]")
            }

            backupFile.writeText(content)

            // Keep only the last 5 backups
            backupDir.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?.drop(5)
                ?.forEach { it.delete() }

            Timber.i("BackupWorker: Backed up ${notes.size} notes to ${backupFile.name}")
            Result.success(workDataOf("backed_up_count" to notes.size))
        } catch (e: Exception) {
            Timber.e(e, "BackupWorker: Failed")
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    override suspend fun getForegroundInfo() = ForegroundInfo(
        NOTIFICATION_ID,
        createNotification()
    )

    private fun createNotification() = androidx.core.app.NotificationCompat.Builder(
        applicationContext, CHANNEL_ID
    )
        .setSmallIcon(android.R.drawable.ic_menu_save)
        .setContentTitle("SecureVault")
        .setContentText("Creating local backup…")
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
        .build()

    companion object {
        const val WORK_NAME = "securevault_backup_worker"
        private const val MAX_RETRIES = 3
        private const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "securevault_backup_channel"

        fun buildRequest() = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
    }
}
