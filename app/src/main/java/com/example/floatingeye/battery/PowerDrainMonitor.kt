package com.example.floatingeye.battery

import android.content.Context
import android.content.IntentFilter
import android.content.Intent
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

data class PowerDrainTelemetry(
    val currentMa: Int = 0,
    val averageCurrentMa: Int = 0,
    val voltageVolts: Float = 0f,
    val powerWatts: Float = 0f,
    val temperatureCelsius: Float = 0f,
    val isCharging: Boolean = false,
    val batteryPercentage: Int = 0,
    val hasCurrentReading: Boolean = false,
    val hasAverageCurrentReading: Boolean = false,
    val hasVoltageReading: Boolean = false,
    val hasTemperatureReading: Boolean = false,
    val hasBatteryLevelReading: Boolean = false,
    val waveformPoints: List<Float> = emptyList()
)

/**
 * Reads device-level battery sensor data. Per-app battery attribution is not
 * available to this regular app, so this monitor reports only device totals.
 */
class PowerDrainMonitor(
    private val context: Context,
    private val settingsRepository: BatterySettingsRepository
) {
    private val batteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val _telemetry = MutableStateFlow(PowerDrainTelemetry())
    val telemetry: StateFlow<PowerDrainTelemetry> = _telemetry.asStateFlow()

    private val monitorScope = CoroutineScope(Dispatchers.Default)
    private var monitorJob: Job? = null

    private val waveBuffer = ArrayDeque<Float>(30)

    fun start() {
        if (monitorJob != null) return
        monitorJob = monitorScope.launch {
            while (isActive) {
                if (settingsRepository.drainMonitorEnabled.value) {
                    measureTelemetry()
                }
                delay(1000L) // Real-time 1-second refresh rate
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun measureTelemetry() {
        val stickyIntent = ContextCompat.registerReceiver(
            context,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )

        val level = stickyIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = stickyIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val hasBatteryLevel = level >= 0 && scale > 0
        val pct = if (hasBatteryLevel) ((level * 100) / scale).coerceIn(0, 100) else 0

        val status = stickyIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val voltageMv = stickyIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val hasVoltage = voltageMv > 0
        val voltageVolts = if (hasVoltage) voltageMv / 1000f else 0f

        val tempTenths = stickyIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val hasTemperature = tempTenths > 0
        val temperatureCelsius = if (hasTemperature) tempTenths / 10f else 0f

        // Android returns Integer.MIN_VALUE when a current property is unsupported.
        var rawCurrentUa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        var rawAvgUa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val hasCurrent = rawCurrentUa != Integer.MIN_VALUE
        val hasAverageCurrent = rawAvgUa != Integer.MIN_VALUE

        // Convert µA to mA
        val currentMa = if (hasCurrent) abs(rawCurrentUa / 1000) else 0
        val avgMa = if (hasAverageCurrent) abs(rawAvgUa / 1000) else 0

        val powerWatts = if (hasCurrent && hasVoltage) {
            voltageVolts * (currentMa / 1000f)
        } else {
            0f
        }

        if (hasCurrent) {
            if (waveBuffer.size >= 30) waveBuffer.removeFirst()
            waveBuffer.addLast((currentMa / 2000f).coerceIn(0f, 1f))
        }

        _telemetry.value = PowerDrainTelemetry(
            currentMa = currentMa,
            averageCurrentMa = avgMa,
            voltageVolts = voltageVolts,
            powerWatts = powerWatts,
            temperatureCelsius = temperatureCelsius,
            isCharging = isCharging,
            batteryPercentage = pct,
            hasCurrentReading = hasCurrent,
            hasAverageCurrentReading = hasAverageCurrent,
            hasVoltageReading = hasVoltage,
            hasTemperatureReading = hasTemperature,
            hasBatteryLevelReading = hasBatteryLevel,
            waveformPoints = waveBuffer.toList()
        )
    }
}
