package com.rocketsauce83.biovarmenne

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {

            val pinStorage = SecurePinStorage(context)

            // If the user has toggled the app off, do nothing
            if (!pinStorage.isEnabled()) return

            // If the app is enabled but the accessibility service is disabled in settings, warn the user
            if (!isAccessibilityServiceEnabled(context)) {
                showServiceStoppedNotification(context)
            }

            // Note: If both are enabled, merely executing this receiver is enough
            // to wake the app process, prompting Android to initialize the AccessibilityService.
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(context.packageName)
    }

    private fun showServiceStoppedNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context,
            BiovarmenneAccessibilityService.NOTIFICATION_CHANNEL_SERVICE_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_service_stopped_title))
            .setContentText(context.getString(R.string.notification_service_stopped_message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context)
                    .notify(BiovarmenneAccessibilityService.NOTIFICATION_ID_SERVICE_STOPPED, notification)
            }
        } else {
            NotificationManagerCompat.from(context)
                .notify(BiovarmenneAccessibilityService.NOTIFICATION_ID_SERVICE_STOPPED, notification)
        }
    }
}