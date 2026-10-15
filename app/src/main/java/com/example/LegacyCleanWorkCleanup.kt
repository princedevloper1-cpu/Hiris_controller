package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager

/** One-time cancellation of background jobs from the removed Hiris Clean extension. */
object LegacyCleanWorkCleanup {
    private const val PREFS = "hiris_legacy_cleanup"
    private const val KEY_DONE = "clean_work_cancelled"

    fun run(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.cancelUniqueWork("hiris_clean_daily_check")
        workManager.cancelAllWorkByTag("com.example.floatingeye.clean.CleanBackgroundWorker")
        workManager.cancelAllWorkByTag("com.example.floatingeye.clean.CleanAppReminderWorker")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notifications = NotificationManagerCompat.from(context)
            notifications.activeNotifications
                .filter { Build.VERSION.SDK_INT < Build.VERSION_CODES.O || it.notification.channelId == "hiris_clean_notifications" }
                .forEach { notifications.cancel(it.id) }
        }
        val folderPrefs = context.getSharedPreferences("hiris_clean_folders", Context.MODE_PRIVATE)
        folderPrefs.getString("downloads_tree_uri", null)?.let { rawUri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(rawUri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        listOf(
            "hiris_clean_folders",
            "hiris_clean_protected_items",
            "hiris_clean_notification_settings",
            "hiris_clean_scan_cache"
        ).forEach { name -> context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply() }
        context.getSharedPreferences("hiris_extensions_prefs", Context.MODE_PRIVATE)
            .edit().remove("ext_active_hiris_clean").apply()
        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }
}
