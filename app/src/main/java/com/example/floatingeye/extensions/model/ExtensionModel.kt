package com.example.floatingeye.extensions.model

enum class ExtensionStatus {
    ACTIVE,
    OFF,
    NEEDS_ACCESS
}

enum class ExtensionCategory {
    ALL,
    PRODUCTIVITY,
    VISUALIZATION,
    SYSTEM,
    CUSTOMIZATION,
    UTILITIES
}

data class HirisExtension(
    val id: String,
    val name: String,
    val description: String,
    val categoryTags: List<String>,
    val category: ExtensionCategory,
    val iconKey: String,
    val isActive: Boolean,
    val version: String = "2.4",
    val author: String = "HIRIS Core"
) {
    val status: ExtensionStatus
        get() = when {
            isActive -> ExtensionStatus.ACTIVE
            else -> ExtensionStatus.OFF
        }
}
