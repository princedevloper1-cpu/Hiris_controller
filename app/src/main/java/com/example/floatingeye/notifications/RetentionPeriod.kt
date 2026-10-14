package com.example.floatingeye.notifications

/**
 * Retention period settings for auto-deleting captured notifications and media.
 * Supported user choices: 8 Hours, 24 Hours, 1 Week (7 Days), or Keep Forever.
 */
enum class RetentionPeriod(
    val durationHours: Long,
    val code: String,
    val label: String
) {
    HOURS_8(8L, "8h", "8 Hours"),
    HOURS_24(24L, "24h", "24 Hours"),
    WEEK_1(24L * 7L, "1week", "1 Week (7 Days)"),
    NEVER(-1L, "never", "Keep Forever");

    val labelCreole: String get() = label
    val labelEn: String get() = label

    companion object {
        fun fromCode(code: String?): RetentionPeriod {
            return entries.firstOrNull {
                it.code.equals(code, ignoreCase = true) || (code == "1semaine" && it == WEEK_1)
            } ?: HOURS_24
        }
    }
}
