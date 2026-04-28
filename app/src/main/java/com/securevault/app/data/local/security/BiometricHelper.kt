package com.securevault.app.data.local.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed class BiometricResult {
    data object Success : BiometricResult()
    data class Error(val code: Int, val message: String) : BiometricResult()
    data object Failed : BiometricResult()
    data class CryptoSuccess(val cryptoObject: BiometricPrompt.CryptoObject) : BiometricResult()
}

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    UNKNOWN_ERROR
}

@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun checkAvailability(): BiometricAvailability {
        return when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS          -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            else -> BiometricAvailability.UNKNOWN_ERROR
        }
    }

    fun canAuthenticate(): Boolean =
        checkAvailability() == BiometricAvailability.AVAILABLE

    /**
     * Show a standard biometric prompt. Returns a Flow<BiometricResult> that emits
     * exactly one item and then closes.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock SecureVault",
        subtitle: String = "Confirm your identity to continue",
        negativeButtonText: String = "Cancel",
        cryptoObject: BiometricPrompt.CryptoObject? = null
    ): Flow<BiometricResult> = callbackFlow {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Timber.d("BiometricHelper: Authentication succeeded")
                val bioResult = result.cryptoObject?.let { BiometricResult.CryptoSuccess(it) }
                    ?: BiometricResult.Success
                trySend(bioResult)
                close()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Timber.w("BiometricHelper: Auth error $errorCode – $errString")
                trySend(BiometricResult.Error(errorCode, errString.toString()))
                close()
            }

            override fun onAuthenticationFailed() {
                Timber.w("BiometricHelper: Authentication failed")
                trySend(BiometricResult.Failed)
                // Do NOT close – user may retry
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        if (cryptoObject != null) {
            prompt.authenticate(promptInfo, cryptoObject)
        } else {
            prompt.authenticate(promptInfo)
        }

        awaitClose { /* BiometricPrompt cancels on lifecycle; no-op needed */ }
    }
}
