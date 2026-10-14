package com.example.floatingeye.battery

import androidx.compose.ui.graphics.Color

enum class BatteryHealthState(val label: String) {
    NORMAL("Normal"),
    LOW("Low Battery (15% - 29%)"),
    CRITICAL("Critical (5% - 14%)"),
    VERY_CRITICAL("Very Critical (< 5%)")
}

data class BatteryInfo(
    val percentage: Int = 100,
    val isCharging: Boolean = false,
    val isFull: Boolean = false
) {
    val healthState: BatteryHealthState
        get() = when {
            percentage < 5 -> BatteryHealthState.VERY_CRITICAL
            percentage < 15 -> BatteryHealthState.CRITICAL
            percentage < 30 -> BatteryHealthState.LOW
            else -> BatteryHealthState.NORMAL
        }
}

object BatteryColors {
    // 80% - 100% & 30% - 79% Normal
    val NeonCyanNormal = Color(0xFF00CCFF)
    val IrisBlue = Color(0xFF0077FF)

    // 15% - 29% Low Battery
    val NeonAmberLow = Color(0xFFFFB020)

    // 5% - 14% Critical
    val NeonRedCritical = Color(0xFFFF3B30)
    val IrisRed = Color(0xFFFF3B30)

    // Charging
    val NeonElectricCyan = Color(0xFF00E5FF)

    // Fully Charged (100%)
    val GreenAccent = Color(0xFF38E07B)
}
