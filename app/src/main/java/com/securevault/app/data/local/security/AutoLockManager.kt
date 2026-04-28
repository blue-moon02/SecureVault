package com.securevault.app.data.local.security

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.securevault.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes the app's process lifecycle and triggers a lock if the app has been
 * backgrounded longer than the user's auto-lock timeout.
 *
 * Usage: call [start] from Application.onCreate().
 */
@Singleton
class AutoLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked

    private var backgroundedAt: Long = 0L

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Timber.d("AutoLockManager: Started")
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundedAt = System.currentTimeMillis()
        Timber.d("AutoLockManager: App backgrounded")
    }

    override fun onStart(owner: LifecycleOwner) {
        if (backgroundedAt == 0L) return
        scope.launch {
            val settings = settingsRepository.getSettings().first()
            if (!settings.isBiometricEnabled) return@launch
            val elapsedSeconds = (System.currentTimeMillis() - backgroundedAt) / 1000L
            if (elapsedSeconds >= settings.autoLockTimeoutSeconds) {
                Timber.i("AutoLockManager: Locking after ${elapsedSeconds}s in background")
                _isLocked.value = true
            }
        }
    }

    fun unlock() {
        _isLocked.value = false
        backgroundedAt = 0L
        scope.launch {
            settingsRepository.updateLastUnlocked(System.currentTimeMillis())
        }
    }
}
