package com.example.floatingeye.tasks.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "HIRIS Task Reminder"
        val taskDescription = intent.getStringExtra(EXTRA_TASK_DESC) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Prefer Alarm sound (loud ringtone) with fallbacks
        val selectedSound = intent.getStringExtra(EXTRA_RINGTONE_URI)
            ?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() }
        val soundUri = selectedSound ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val vibrationPattern = longArrayOf(0, 600, 250, 600, 250, 800)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        // Create Channel with Alarm Sound & Vibration if Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelIdFor(soundUri),
                "HIRIS Task Reminders & Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders, ringing alerts and sound notifications for tasks"
                enableVibration(true)
                this.vibrationPattern = vibrationPattern
                enableLights(true)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_extension_id", "tasks")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) channelIdFor(soundUri) else CHANNEL_ID
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ Rappel Tach: $taskTitle")
            .setContentText(if (taskDescription.isNotBlank()) taskDescription else "Ou gen yon tach pou w fè kounye a!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(if (taskDescription.isNotBlank()) "$taskDescription\n\nKlike pou w wè oswa make li fini nan HIRIS." else "Ou gen yon tach pou w fè kounye a! Klike pou w wè oswa make li fini.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setSound(soundUri).setVibrate(vibrationPattern).setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Reminder fired but HIRIS notifications are disabled by the user")
            return
        }
        runCatching {
            notificationManager.notify(taskId.hashCode(), notificationBuilder.build())
        }.onFailure { error ->
            Log.e(TAG, "Unable to display task reminder", error)
        }
    }

    companion object {
        const val CHANNEL_ID = "hiris_task_reminders_v4"
        private const val TAG = "HirisTaskReminder"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESC = "extra_task_desc"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
        private fun channelIdFor(soundUri: android.net.Uri?): String {
            val soundKey = soundUri?.toString()?.hashCode()?.let(Integer::toHexString) ?: "default"
            return "hiris_task_v4_$soundKey"
        }
    }
}
