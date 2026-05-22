package com.rocketsauce83.biovarmenne

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePinStorage(context: Context) {

    companion object {
        private const val KEY_ALIAS = "biovarmenne_pin_key"
        private const val PREFS_FILE = "biovarmenne_secure_prefs"
        private const val PIN_KEY = "user_pin"
    }

    private val masterKey = MasterKey.Builder(context, KEY_ALIAS)
        .setKeyGenParameterSpec(
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
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePin(pin: String) {
        securePrefs.edit {
            putString(PIN_KEY, pin)
        }
    }

    fun getPin(): String? {
        return try {
            securePrefs.getString(PIN_KEY, null)
        } catch (_: Exception) {
            null
        }
    }

    fun hasPin(): Boolean {
        return try {
            securePrefs.contains(PIN_KEY)
        } catch (_: Exception) {
            false
        }
    }

    fun clearPin() {
        securePrefs.edit {
            remove(PIN_KEY)
        }
    }
}