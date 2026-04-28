package com.securevault.app.data.local.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles AES-256-GCM encryption/decryption using the Android Keystore.
 * Keys never leave secure hardware (on supported devices).
 */
@Singleton
class CryptoManager @Inject constructor() {

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    // ---- DB passphrase key ----
    fun getOrCreateDbKey(alias: String = DB_KEY_ALIAS): SecretKey {
        return keyStore.getKey(alias, null) as? SecretKey
            ?: generateKey(alias, requireUserAuth = false)
    }

    // ---- Note content encryption key (requires biometric) ----
    fun getOrCreateNoteKey(alias: String = NOTE_KEY_ALIAS): SecretKey {
        return keyStore.getKey(alias, null) as? SecretKey
            ?: generateKey(alias, requireUserAuth = true)
    }

    fun encrypt(data: ByteArray, key: SecretKey): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(data)
        return EncryptedData(iv = iv, ciphertext = ciphertext)
    }

    fun decrypt(encryptedData: EncryptedData, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(encryptedData.ciphertext)
    }

    fun getCipherForEncryption(keyAlias: String): Cipher {
        val key = getOrCreateNoteKey(keyAlias)
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    fun getCipherForDecryption(keyAlias: String, iv: ByteArray): Cipher {
        val key = getOrCreateNoteKey(keyAlias)
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
    }

    fun deleteKey(alias: String) {
        runCatching {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
                Timber.d("CryptoManager: Deleted key $alias")
            }
        }.onFailure { Timber.e(it, "CryptoManager: Failed to delete key $alias") }
    }

    private fun generateKey(alias: String, requireUserAuth: Boolean): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (requireUserAuth) {
            builder
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(
                    0, // 0 = require every use
                    KeyProperties.AUTH_BIOMETRIC_STRONG
                )
                .setInvalidatedByBiometricEnrollment(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    data class EncryptedData(val iv: ByteArray, val ciphertext: ByteArray) {
        /** Serialize as iv_length(4 bytes) + iv + ciphertext */
        fun toByteArray(): ByteArray {
            val ivLen = iv.size
            return ByteArray(4 + ivLen + ciphertext.size).also { buf ->
                buf[0] = (ivLen shr 24).toByte()
                buf[1] = (ivLen shr 16).toByte()
                buf[2] = (ivLen shr 8).toByte()
                buf[3] = ivLen.toByte()
                System.arraycopy(iv, 0, buf, 4, ivLen)
                System.arraycopy(ciphertext, 0, buf, 4 + ivLen, ciphertext.size)
            }
        }

        companion object {
            fun fromByteArray(bytes: ByteArray): EncryptedData {
                val ivLen = ((bytes[0].toInt() and 0xFF) shl 24) or
                            ((bytes[1].toInt() and 0xFF) shl 16) or
                            ((bytes[2].toInt() and 0xFF) shl 8) or
                             (bytes[3].toInt() and 0xFF)
                val iv = bytes.copyOfRange(4, 4 + ivLen)
                val ciphertext = bytes.copyOfRange(4 + ivLen, bytes.size)
                return EncryptedData(iv, ciphertext)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EncryptedData) return false
            return iv.contentEquals(other.iv) && ciphertext.contentEquals(other.ciphertext)
        }

        override fun hashCode() = 31 * iv.contentHashCode() + ciphertext.contentHashCode()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val DB_KEY_ALIAS = "securevault_db_master_key"
        const val NOTE_KEY_ALIAS = "securevault_note_content_key"
    }
}
