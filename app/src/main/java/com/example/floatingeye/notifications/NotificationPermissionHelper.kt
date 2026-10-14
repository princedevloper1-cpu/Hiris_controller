package com.example.floatingeye.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object NotificationPermissionHelper {

    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        if (enabledPackages.contains(context.packageName)) {
            return true
        }

        // Secondary verification via secure settings string
        return try {
            val componentName = ComponentName(context, HirisNotificationListenerService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            flat != null && flat.contains(componentName.flattenToString())
        } catch (_: Exception) {
            false
        }
    }

    fun openNotificationListenerSettings(context: Context) {
        val componentName = ComponentName(context, HirisNotificationListenerService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componentName.flattenToString())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
