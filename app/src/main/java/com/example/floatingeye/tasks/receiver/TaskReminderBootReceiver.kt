package com.example.floatingeye.tasks.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.floatingeye.tasks.repository.TaskRepository

class TaskReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent.action != "android.intent.action.TIME_SET" &&
            intent.action != "android.intent.action.TIMEZONE_CHANGED" &&
            intent.action != "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        ) return

        val pendingResult = goAsync()
        Thread {
            try {
                val repository = TaskRepository.getInstance(context.applicationContext)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    (context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager)
                        ?.canScheduleExactAlarms() == true
                ) {
                    repository.reschedulePendingAlarms()
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
