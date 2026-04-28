package com.securevault.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.securevault.app.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object Keys {
        val BIOMETRIC_ENABLED       = booleanPreferencesKey("biometric_enabled")
        val AUTO_LOCK_TIMEOUT       = intPreferencesKey("auto_lock_timeout_seconds")
        val PANIC_MODE              = booleanPreferencesKey("panic_mode")
        val DYNAMIC_COLOR           = booleanPreferencesKey("dynamic_color")
        val SCREENSHOT_PROTECTION   = booleanPreferencesKey("screenshot_protection")
        val LAST_UNLOCKED_AT        = longPreferencesKey("last_unlocked_at")
        val IS_DARK_MODE            = stringPreferencesKey("dark_mode")  // "true"/"false"/"system"
    }

    val appSettings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "DataStore read error, emitting defaults")
                emit(emptyPreferences())
            } else throw exception
        }
        .map { prefs ->
            AppSettings(
                isBiometricEnabled          = prefs[Keys.BIOMETRIC_ENABLED] ?: true,
                autoLockTimeoutSeconds      = prefs[Keys.AUTO_LOCK_TIMEOUT] ?: 30,
                panicModeEnabled            = prefs[Keys.PANIC_MODE] ?: false,
                dynamicColorEnabled         = prefs[Keys.DYNAMIC_COLOR] ?: true,
                screenshotProtectionEnabled = prefs[Keys.SCREENSHOT_PROTECTION] ?: true,
                lastUnlockedAt              = prefs[Keys.LAST_UNLOCKED_AT] ?: 0L,
                isDarkMode = when (prefs[Keys.IS_DARK_MODE]) {
                    "true"  -> true
                    "false" -> false
                    else    -> null
                }
            )
        }

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED]       = settings.isBiometricEnabled
            prefs[Keys.AUTO_LOCK_TIMEOUT]        = settings.autoLockTimeoutSeconds
            prefs[Keys.PANIC_MODE]               = settings.panicModeEnabled
            prefs[Keys.DYNAMIC_COLOR]            = settings.dynamicColorEnabled
            prefs[Keys.SCREENSHOT_PROTECTION]    = settings.screenshotProtectionEnabled
            prefs[Keys.LAST_UNLOCKED_AT]         = settings.lastUnlockedAt
            prefs[Keys.IS_DARK_MODE]             = when (settings.isDarkMode) {
                true  -> "true"
                false -> "false"
                null  -> "system"
            }
        }
    }

    suspend fun updateLastUnlocked(timestamp: Long) {
        dataStore.edit { it[Keys.LAST_UNLOCKED_AT] = timestamp }
    }
}
