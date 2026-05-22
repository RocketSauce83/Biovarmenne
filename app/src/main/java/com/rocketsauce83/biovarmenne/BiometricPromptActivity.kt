package com.rocketsauce83.biovarmenne

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
                // BiometricPrompt handles retry UI automatically
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // 1. Tell the service to handle the STK cancellation and its own 2000ms cooldown
                BiovarmenneEvents.sendCancelStk()

                // 2. Immediately finish this activity so STK becomes the active window again.
                // Do NOT send resetGuard here, as it will overwrite the service's cooldown.
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