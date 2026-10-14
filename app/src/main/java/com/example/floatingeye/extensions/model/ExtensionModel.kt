package com.example.floatingeye.extensions.model

enum class ExtensionStatus {
    ACTIVE,
    OFF,
    NEW
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
    val isInstalled: Boolean,
    val isActive: Boolean,
    val isNew: Boolean = false,
    val version: String = "2.4",
    val author: String = "HIRIS Core"
) {
    val status: ExtensionStatus
        get() = when {
            !isInstalled && isNew -> ExtensionStatus.NEW
            isInstalled && isActive -> ExtensionStatus.ACTIVE
            else -> ExtensionStatus.OFF
        }
}
