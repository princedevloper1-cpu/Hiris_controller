package com.example.floatingeye.tasks.model

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}

data class HirisTask(
    val id: String,
    val title: String,
    val description: String = "",
    val dueDateMillis: Long, // timestamp when task reminder fires
    val ringtoneUri: String? = null,
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val createdAtMillis: Long = System.currentTimeMillis()
)
