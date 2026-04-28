package com.securevault.app.presentation.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.app.data.local.security.BiometricAvailability
import com.securevault.app.data.local.security.BiometricHelper
import com.securevault.app.data.local.security.EncryptedPreferencesManager
import com.securevault.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.UNKNOWN_ERROR,
    val failedAttempts: Int = 0,
    val isPanicMode: Boolean = false,
    val error: String? = null
)

sealed class AuthEvent {
    data object AuthSuccess : AuthEvent()
    data object PanicActivated : AuthEvent()
    data class ShowError(val message: String) : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val biometricHelper: BiometricHelper,
    private val encryptedPrefs: EncryptedPreferencesManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    init {
        checkBiometricAvailability()
        loadFailedAttempts()
    }

    private fun checkBiometricAvailability() {
        _uiState.update { it.copy(biometricAvailability = biometricHelper.checkAvailability()) }
    }

    private fun loadFailedAttempts() {
        val count = encryptedPrefs.getInt(EncryptedPreferencesManager.Keys.FAILED_AUTH_ATTEMPTS)
        _uiState.update { it.copy(failedAttempts = count) }
    }

    fun onAuthenticationSuccess() {
        viewModelScope.launch {
            encryptedPrefs.putInt(EncryptedPreferencesManager.Keys.FAILED_AUTH_ATTEMPTS, 0)
            settingsRepository.updateLastUnlocked(System.currentTimeMillis())
            _uiState.update { it.copy(failedAttempts = 0) }
            _events.emit(AuthEvent.AuthSuccess)
        }
    }

    fun onAuthenticationFailed() {
        val newCount = _uiState.value.failedAttempts + 1
        encryptedPrefs.putInt(EncryptedPreferencesManager.Keys.FAILED_AUTH_ATTEMPTS, newCount)
        _uiState.update { it.copy(failedAttempts = newCount) }

        // Panic mode: 5 consecutive failures hides sensitive content
        if (newCount >= EncryptedPreferencesManager.MAX_FAILED_ATTEMPTS) {
            activatePanicMode()
        }

        Timber.w("AuthViewModel: Failed attempt #$newCount")
    }

    private fun activatePanicMode() {
        viewModelScope.launch {
            encryptedPrefs.putBoolean(EncryptedPreferencesManager.Keys.PANIC_MODE_ENABLED, true)
            _uiState.update { it.copy(isPanicMode = true) }
            _events.emit(AuthEvent.PanicActivated)
            Timber.w("AuthViewModel: PANIC MODE ACTIVATED")
        }
    }

    fun onAuthError(code: Int, message: String) {
        Timber.w("AuthViewModel: Auth error $code – $message")
        viewModelScope.launch { _events.emit(AuthEvent.ShowError(message)) }
    }
}
