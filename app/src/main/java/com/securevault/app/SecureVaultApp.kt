package com.securevault.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.securevault.app.data.local.security.AutoLockManager
import com.securevault.app.data.worker.BackupWorker
import com.securevault.app.data.worker.CleanupWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SecureVaultApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var autoLockManager: AutoLockManager

    override fun onCreate() {
        super.onCreate()

        // Logging (only in debug builds)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Start auto-lock lifecycle observer
        autoLockManager.start()

        // Create notification channels (required for WorkManager foreground services on API 26+)
        createNotificationChannels()

        // Schedule periodic WorkManager jobs
        scheduleBackgroundWork()
    }

    // Provide WorkManager configuration with Hilt's custom WorkerFactory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    BackupWorker.CHANNEL_ID,
                    "Backup",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Local backup notifications" }
            )
        }
    }

    private fun scheduleBackgroundWork() {
        val workManager = WorkManager.getInstance(this)

        // Cleanup expired notes — runs once a week
        workManager.enqueueUniquePeriodicWork(
            CleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            CleanupWorker.buildRequest()
        )

        // Local backup — runs once a day
        workManager.enqueueUniquePeriodicWork(
            BackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            BackupWorker.buildRequest()
        )
    }
}
