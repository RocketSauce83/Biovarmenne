package com.rocketsauce83.biovarmennepro

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurePinStorage(context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "biovarmenne_pin_key"
        private const val PREFS_FILE = "biovarmenne_secure_prefs"
        private const val PIN_KEY = "user_pin"
        private const val IV_KEY = "user_pin_iv"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        // Return existing key if available
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        // Create new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .setUserAuthenticationParameters(
                    30,
                    KeyProperties.AUTH_BIOMETRIC_STRONG
                )
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("biovarmenne_enabled", enabled) }
    }

    fun isEnabled(): Boolean {
        return prefs.getBoolean("biovarmenne_enabled", true) // default true
    }

    fun savePin(pin: String) {
        try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val encrypted = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))

            prefs.edit {
                putString(PIN_KEY, Base64.encodeToString(encrypted, Base64.DEFAULT))
                putString(IV_KEY, Base64.encodeToString(iv, Base64.DEFAULT))
            }
        } catch (_: Exception) {
            // Encryption failed
        }
    }

    fun getPin(): String? {
        return try {
            val encryptedPin = prefs.getString(PIN_KEY, null) ?: return null
            val ivString = prefs.getString(IV_KEY, null) ?: return null

            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivString, Base64.DEFAULT)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            val decrypted = cipher.doFinal(Base64.decode(encryptedPin, Base64.DEFAULT))
            String(decrypted, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun hasPin(): Boolean {
        return prefs.contains(PIN_KEY)
    }

    fun clearPin() {
        prefs.edit {
            remove(PIN_KEY)
            remove(IV_KEY)
        }
        // Also delete the keystore key
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (_: Exception) {
            // Key deletion failed
        }
    }

    fun migrateIfNeeded() {
        val hasNewFormat = prefs.contains(IV_KEY)
        val hasOldFormat = prefs.contains(PIN_KEY) && !hasNewFormat

        if (hasOldFormat) {
            prefs.edit {
                clear()
            }
            try {
                val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
                keyStore.deleteEntry(KEY_ALIAS)
            } catch (_: Exception) {
                // ignore
            }
        }
    }
}