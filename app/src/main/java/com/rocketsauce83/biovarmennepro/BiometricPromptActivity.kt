package com.rocketsauce83.biovarmennepro

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class BiometricPromptActivity : AppCompatActivity() {

    private lateinit var pinStorage: SecurePinStorage

    private val stkTimeoutHandler = Handler(Looper.getMainLooper())
    private val stkTimeoutRunnable = Runnable {
        BiovarmenneEvents.sendResetGuard()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinStorage = SecurePinStorage(this)
        showBiometricPrompt()
    }

    override fun onResume() {
        super.onResume()
        stkTimeoutHandler.postDelayed(stkTimeoutRunnable, 30000)
    }

    override fun onPause() {
        super.onPause()
        stkTimeoutHandler.removeCallbacks(stkTimeoutRunnable)
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                val pin = pinStorage.getPin() ?: run {
                    finish()
                    return
                }
                finish()
                BiovarmenneEvents.sendPin(pin)
            }

            override fun onAuthenticationFailed() {
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                BiovarmenneEvents.sendCancelStk()

                finish()
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_cancel))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        BiometricPrompt(this, executor, callback).authenticate(promptInfo)
    }
}