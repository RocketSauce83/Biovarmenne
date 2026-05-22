package com.rocketsauce83.biovarmenne

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BiovarmenneAccessibilityService : AccessibilityService() {

    companion object {
        private const val STK_PACKAGE = "com.android.stk"
        private val PIN_PROMPT_TEXTS = listOf(
            "Anna tunnusluku",
            "Ange PIN-kod",
            "Enter PIN"
        )
    }

    private lateinit var pinStorage: SecurePinStorage
    private var isBiometricPromptShowing = false
    private var isCancelling = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()
        pinStorage = SecurePinStorage(this)

        serviceScope.launch {
            BiovarmenneEvents.fillPin.collect { pin ->
                fillPinAndConfirm(pin)
            }
        }

        serviceScope.launch {
            BiovarmenneEvents.resetGuard.collect {
                isBiometricPromptShowing = false
                isCancelling = false
            }
        }

        serviceScope.launch {
            BiovarmenneEvents.cancelStk.collect {
                cancelStkPrompt()
            }
        }

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageNames = arrayOf(STK_PACKAGE)
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        android.util.Log.d("Biovarmenne", "Event received: ${event.packageName}")
        if (event.packageName != STK_PACKAGE) return
        if (isBiometricPromptShowing) return
        if (isCancelling) return

        val rootNode = rootInActiveWindow ?: return

        if (isPinPromptVisible(rootNode)) {
            handlePinPrompt()
        }
    }

    private fun isPinPromptVisible(rootNode: AccessibilityNodeInfo): Boolean {
        return PIN_PROMPT_TEXTS.any { promptText ->
            rootNode.findAccessibilityNodeInfosByText(promptText).isNotEmpty()
        }
    }

    private fun handlePinPrompt() {
        if (!pinStorage.hasPin()) return

        isBiometricPromptShowing = true

        val intent = Intent(this, BiometricPromptActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        startActivity(intent)
    }

    private fun fillPinAndConfirm(pin: String) {
        val handler = Handler(Looper.getMainLooper())
        var attempts = 0
        val maxAttempts = 10

        val runnable = object : Runnable {
            override fun run() {
                attempts++
                val rootNode = rootInActiveWindow

                if (rootNode == null) {
                    if (attempts < maxAttempts) handler.postDelayed(this, 300)
                    else isBiometricPromptShowing = false
                    return
                }

                val packageName = rootNode.packageName?.toString()

                if (packageName != STK_PACKAGE) {
                    if (attempts < maxAttempts) handler.postDelayed(this, 300)
                    else isBiometricPromptShowing = false
                    return
                }

                val inputField = findInputField(rootNode)

                if (inputField == null) {
                    isBiometricPromptShowing = false
                    return
                }

                val arguments = Bundle().apply {
                    putString(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        pin
                    )
                }
                inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                handler.postDelayed({
                    clickOkButton(rootNode)
                    handler.postDelayed({
                        isBiometricPromptShowing = false
                    }, 2000)
                }, 300)
            }
        }

        handler.postDelayed(runnable, 300)
    }

    private fun findInputField(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            if (child.className == "android.widget.EditText") {
                return child
            }
            val found = findInputField(child)
            if (found != null) return found
        }
        return null
    }

    private fun clickOkButton(rootNode: AccessibilityNodeInfo) {
        val okTexts = listOf("OK", "Ok")
        okTexts.forEach { text ->
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            nodes.firstOrNull()?.let { node ->
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }
    }

    private fun cancelStkPrompt() {
        isCancelling = true
        val handler = Handler(Looper.getMainLooper())
        var attempts = 0
        val maxAttempts = 5

        val runnable = object : Runnable {
            override fun run() {
                attempts++
                val rootNode = rootInActiveWindow

                if (rootNode == null) {
                    if (attempts < maxAttempts) handler.postDelayed(this, 200)
                    else isBiometricPromptShowing = false
                    return
                }

                val packageName = rootNode.packageName?.toString()

                if (packageName != STK_PACKAGE) {
                    // BiometricPromptActivity still in foreground, wait for it to finish
                    if (attempts < maxAttempts) handler.postDelayed(this, 200)
                    else isBiometricPromptShowing = false
                    return
                }

                val cancelTexts = listOf("Peru", "PERU", "Cancel", "Avbryt")
                cancelTexts.forEach { text ->
                    val nodes = rootNode.findAccessibilityNodeInfosByText(text)
                    nodes.firstOrNull()?.let { node ->
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        // 👇 Delay reset to let STK fully close first
                        handler.postDelayed({
                            isCancelling = false
                            isBiometricPromptShowing = false
                        }, 2000)
                        return
                    }
                }
                isCancelling = false
                isBiometricPromptShowing = false
            }
        }

        // Start immediately
        handler.post(runnable)
    }

    override fun onInterrupt() {}
}