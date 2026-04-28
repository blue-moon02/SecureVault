package com.securevault.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.securevault.app.domain.repository.NoteRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodically deletes expired notes. Scheduled weekly via WorkManager.
 */
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val noteRepository: NoteRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val deleted = noteRepository.deleteExpiredNotes()
            Timber.i("CleanupWorker: Deleted $deleted expired notes")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "CleanupWorker: Failed")
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "securevault_cleanup_worker"
        private const val MAX_RETRIES = 3

        fun buildRequest() = PeriodicWorkRequestBuilder<CleanupWorker>(7, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()
    }
}
