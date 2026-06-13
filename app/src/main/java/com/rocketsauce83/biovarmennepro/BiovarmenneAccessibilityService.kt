package com.rocketsauce83.biovarmennepro

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@SuppressLint("AccessibilityPolicy")
class BiovarmenneAccessibilityService : AccessibilityService() {

    companion object {
        private val STK_PACKAGES = listOf("com.android.stk", "com.android.stk2")
        private const val NOTIFICATION_CHANNEL_ID = "biovarmenne_wrong_pin"
        const val NOTIFICATION_CHANNEL_SERVICE_ID = "biovarmenne_service_status"
        private const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_ID_SERVICE_STOPPED = 1002
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
        createNotificationChannel()
        cancelServiceStoppedNotification()

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
            packageNames = STK_PACKAGES.toTypedArray()
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (pinStorage.isEnabled()) {
            showServiceStoppedNotification()
        }
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Existing wrong PIN channel
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Biovarmenne",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Biovarmenne PIN notifications"
            }
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_SERVICE_ID,
                getString(R.string.notification_channel_service_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_service_desc)
            }
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!pinStorage.isEnabled()) return
        if (BuildConfig.DEBUG) {
            android.util.Log.d("Biovarmenne", "Event received: ${event.packageName}")
        }
        if (event.packageName?.toString() !in STK_PACKAGES) return
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

                if (packageName !in STK_PACKAGES) {
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
                        checkIfPinWasWrong()
                    }, 3000)
                }, 300)
            }
        }

        handler.postDelayed(runnable, 300)
    }

    private fun checkIfPinWasWrong() {
        val rootNode = rootInActiveWindow ?: run {
            isBiometricPromptShowing = false
            return
        }

        if (STK_PACKAGES.contains(rootNode.packageName?.toString()) &&
            isPinPromptVisible(rootNode)) {
            isBiometricPromptShowing = false
            showWrongPinNotification()
        } else {
            isBiometricPromptShowing = false
        }
    }

    private fun showWrongPinNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_wrong_pin_title))
            .setContentText(getString(R.string.notification_wrong_pin_message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
            }
        } else {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun showServiceStoppedNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_SERVICE_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_service_stopped_title))
            .setContentText(getString(R.string.notification_service_stopped_message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this)
                    .notify(NOTIFICATION_ID_SERVICE_STOPPED, notification)
            }
        } else {
            NotificationManagerCompat.from(this)
                .notify(NOTIFICATION_ID_SERVICE_STOPPED, notification)
        }
    }

    private fun cancelServiceStoppedNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_SERVICE_STOPPED)
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

                if (packageName !in STK_PACKAGES) {
                    if (attempts < maxAttempts) handler.postDelayed(this, 200)
                    else isBiometricPromptShowing = false
                    return
                }

                val cancelTexts = listOf(
                    "Peru", "PERU",
                    "Peruuta", "PERUUTA",
                    "Cancel", "Avbryt"
                )
                cancelTexts.forEach { text ->
                    val nodes = rootNode.findAccessibilityNodeInfosByText(text)
                    nodes.firstOrNull()?.let { node ->
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
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

        handler.post(runnable)
    }

    override fun onInterrupt() {}
}