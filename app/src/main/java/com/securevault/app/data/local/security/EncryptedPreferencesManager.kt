package com.securevault.app.data.local.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around EncryptedSharedPreferences (AES256-SIV keys + AES256-GCM values).
 * Use for small sensitive config values (e.g., failed-attempt counter, panic flag).
 * For the DB passphrase, use CryptoManager + Android Keystore directly.
 */
@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false) // Prefs unlocked by app startup
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ---- Typed accessors ----
    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getString(key: String, default: String? = null): String? = prefs.getString(key, default)

    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun getInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)

    fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    fun getLong(key: String, default: Long = 0L): Long = prefs.getLong(key, default)

    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)

    fun remove(key: String) = prefs.edit().remove(key).apply()

    fun clearAll() {
        Timber.w("EncryptedPrefs: Clearing all preferences")
        prefs.edit().clear().apply()
    }

    // ---- Well-known keys ----
    object Keys {
        const val FAILED_AUTH_ATTEMPTS = "failed_auth_attempts"
        const val PANIC_MODE_ENABLED   = "panic_mode_enabled"
        const val LAST_UNLOCKED_AT     = "last_unlocked_at"
        const val AUTO_LOCK_TIMEOUT    = "auto_lock_timeout"
        const val BIOMETRIC_ENABLED    = "biometric_enabled"
        const val SCREENSHOT_PROTECTION = "screenshot_protection"
        const val DYNAMIC_COLOR        = "dynamic_color"
    }

    companion object {
        private const val PREFS_FILE_NAME = "securevault_encrypted_prefs"
        const val MAX_FAILED_ATTEMPTS = 5
    }
}
