package com.example.floatingeye.tasks.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.example.floatingeye.tasks.model.HirisTask
import com.example.floatingeye.tasks.model.TaskPriority
import com.example.floatingeye.tasks.model.TaskReminderDraft
import com.example.floatingeye.tasks.receiver.TaskReminderReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class TaskRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _tasks = MutableStateFlow<List<HirisTask>>(loadTasks())
    val tasks: StateFlow<List<HirisTask>> = _tasks.asStateFlow()
    private val _reminderDraft = MutableStateFlow<TaskReminderDraft?>(null)
    val reminderDraft: StateFlow<TaskReminderDraft?> = _reminderDraft.asStateFlow()

    fun setReminderDraft(draft: TaskReminderDraft) { _reminderDraft.value = draft }
    fun clearReminderDraft() { _reminderDraft.value = null }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    init {
        // AlarmManager does not persist alarms across reboot or app process replacement.
        // Restore saved reminders whenever Tasks is first opened in a process.
        reschedulePendingAlarms()
    }

    private fun loadTasks(): List<HirisTask> {
        val jsonString = prefs.getString(KEY_TASKS_JSON, null) ?: return defaultSampleTasks()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<HirisTask>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    HirisTask(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        dueDateMillis = obj.getLong("dueDateMillis"),
                        ringtoneUri = obj.optString("ringtoneUri").takeIf { it.isNotBlank() && it != "null" },
                        isCompleted = obj.optBoolean("isCompleted", false),
                        priority = try {
                            TaskPriority.valueOf(obj.optString("priority", "MEDIUM"))
                        } catch (_: Exception) {
                            TaskPriority.MEDIUM
                        },
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
            list.sortedBy { it.dueDateMillis }
        } catch (_: Exception) {
            defaultSampleTasks()
        }
    }

    private fun defaultSampleTasks(): List<HirisTask> {
        val now = System.currentTimeMillis()
        return listOf(
            HirisTask(
                id = "sample_1",
                title = "Inspect App Shield & Data Blocks",
                description = "Review blocked social media apps and network consumption",
                dueDateMillis = now + 3600_000L * 2,
                priority = TaskPriority.HIGH
            ),
            HirisTask(
                id = "sample_2",
                title = "Audit Bluetooth Mic & Battery Reactions",
                description = "Run latency check and verify arc reactor battery glow",
                dueDateMillis = now + 3600_000L * 5,
                priority = TaskPriority.MEDIUM
            )
        )
    }

    private fun saveTasks(taskList: List<HirisTask>) {
        val jsonArray = JSONArray()
        for (task in taskList) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("dueDateMillis", task.dueDateMillis)
                put("ringtoneUri", task.ringtoneUri)
                put("isCompleted", task.isCompleted)
                put("priority", task.priority.name)
                put("createdAtMillis", task.createdAtMillis)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_TASKS_JSON, jsonArray.toString()).apply()
        _tasks.value = taskList.sortedBy { it.dueDateMillis }
    }

    fun addTask(task: HirisTask): Boolean {
        _tasks.value.firstOrNull { it.id == task.id }?.let(::cancelTaskAlarm)
        val updated = _tasks.value.filter { it.id != task.id } + task
        saveTasks(updated)
        return scheduleTaskAlarm(task)
    }

    fun toggleTaskCompleted(taskId: String) {
        val updated = _tasks.value.map {
            if (it.id == taskId) {
                val newCompleted = !it.isCompleted
                if (newCompleted) {
                    cancelTaskAlarm(it)
                } else {
                    scheduleTaskAlarm(it)
                }
                it.copy(isCompleted = newCompleted)
            } else it
        }
        saveTasks(updated)
    }

    fun deleteTask(taskId: String) {
        val task = _tasks.value.firstOrNull { it.id == taskId }
        if (task != null) {
            cancelTaskAlarm(task)
        }
        val updated = _tasks.value.filter { it.id != taskId }
        saveTasks(updated)
    }

    private fun scheduleTaskAlarm(task: HirisTask): Boolean {
        if (task.isCompleted || task.dueDateMillis <= System.currentTimeMillis()) return true
        val alarmMgr = alarmManager ?: return false

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(TaskReminderReceiver.EXTRA_TASK_DESC, task.description)
            putExtra(TaskReminderReceiver.EXTRA_RINGTONE_URI, task.ringtoneUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmMgr.canScheduleExactAlarms()) {
                    alarmMgr.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.dueDateMillis,
                        pendingIntent
                    )
                } else {
                    alarmMgr.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.dueDateMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmMgr.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    task.dueDateMillis,
                    pendingIntent
                )
            } else {
                alarmMgr.setExact(
                    AlarmManager.RTC_WAKEUP,
                    task.dueDateMillis,
                    pendingIntent
                )
            }
            return true
        } catch (_: Exception) {
            // Fallback in case of system restrictions
            try {
                alarmMgr.set(
                    AlarmManager.RTC_WAKEUP,
                    task.dueDateMillis,
                    pendingIntent
                )
                return true
            } catch (_: Exception) {
                return false
            }
        }
    }

    private fun cancelTaskAlarm(task: HirisTask) {
        val alarmMgr = alarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmMgr.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun reschedulePendingAlarms() {
        _tasks.value.filter { !it.id.startsWith("sample_") && !it.isCompleted && it.dueDateMillis > System.currentTimeMillis() }
            .forEach { scheduleTaskAlarm(it) }
    }

    companion object {
        private const val PREFS_NAME = "hiris_tasks_prefs"
        private const val KEY_TASKS_JSON = "key_tasks_json"

        @Volatile
        private var instance: TaskRepository? = null

        fun getInstance(context: Context): TaskRepository {
            return instance ?: synchronized(this) {
                instance ?: TaskRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
