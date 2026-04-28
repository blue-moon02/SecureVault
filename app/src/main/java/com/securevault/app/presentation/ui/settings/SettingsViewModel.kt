package com.securevault.app.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.app.domain.model.AppSettings
import com.securevault.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository
        .getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateBiometric(enabled: Boolean) = update { it.copy(isBiometricEnabled = enabled) }
    fun updateAutoLock(seconds: Int)       = update { it.copy(autoLockTimeoutSeconds = seconds) }
    fun updatePanicMode(enabled: Boolean)  = update { it.copy(panicModeEnabled = enabled) }
    fun updateDynamicColor(enabled: Boolean) = update { it.copy(dynamicColorEnabled = enabled) }
    fun updateScreenshotProtection(enabled: Boolean) = update { it.copy(screenshotProtectionEnabled = enabled) }
    fun updateDarkMode(dark: Boolean?) = update { it.copy(isDarkMode = dark) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(transform(current))
        }
    }
}
