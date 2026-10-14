package com.example.floatingeye.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.example.floatingeye.controller.FloatingEyeLightController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors Android BatteryManager broadcasts and translates battery health, low power,
 * and charging states into luminous cyan, amber, red, and green responses on FloatingEyeLightController.
 */
class FloatingEyeBatteryController(
    private val context: Context,
    private val lightController: FloatingEyeLightController,
    val settingsRepository: BatterySettingsRepository = BatterySettingsRepository(context)
) {
    private val appContext = context.applicationContext
    val drainMonitor = PowerDrainMonitor(appContext, settingsRepository)

    private val _realBatteryInfo = MutableStateFlow(BatteryInfo())
    private val _effectiveBatteryInfo = MutableStateFlow(BatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _effectiveBatteryInfo.asStateFlow()

    private var isReceiverRegistered = false
    private var lastWasCharging = false
    private var lastWasFull = false

    // Preview simulation override for dashboard testing
    private var simulatedInfo: BatteryInfo? = null

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("floating_eye_battery_prefs", Context.MODE_PRIVATE)

    // Automatically re-apply states when user alters toggles in the dashboard
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        applyCurrentState()
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                parseBatteryIntent(intent)
            }
        }
    }

    init {
        // Read immediate sticky intent on creation
        val stickyIntent = ContextCompat.registerReceiver(
            appContext,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
        stickyIntent?.let { parseBatteryIntent(it) }
    }

    fun startListening() {
        drainMonitor.start()
        if (!isReceiverRegistered) {
            try {
                ContextCompat.registerReceiver(
                    appContext,
                    batteryReceiver,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    ContextCompat.RECEIVER_EXPORTED
                )
                prefs.registerOnSharedPreferenceChangeListener(prefsListener)
                isReceiverRegistered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        applyCurrentState()
    }

    fun stopListening() {
        drainMonitor.stop()
        if (isReceiverRegistered) {
            try {
                appContext.unregisterReceiver(batteryReceiver)
                prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isReceiverRegistered = false
        }
    }

    fun setSimulationOverride(sim: BatteryInfo?) {
        simulatedInfo = sim
        _effectiveBatteryInfo.value = sim ?: _realBatteryInfo.value
        applyCurrentState()
    }

    private fun parseBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (scale > 0) (level * 100) / scale else level

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL || pct >= 100

        val info = BatteryInfo(
            percentage = pct.coerceIn(0, 100),
            isCharging = isCharging,
            isFull = isFull
        )
        _realBatteryInfo.value = info
        if (simulatedInfo == null) {
            _effectiveBatteryInfo.value = info
        }
        applyCurrentState()
    }

    fun applyCurrentState() {
        val info = simulatedInfo ?: _realBatteryInfo.value
        val reactionsEnabled = settingsRepository.reactionsEnabled.value

        if (!reactionsEnabled) {
            // Revert to pristine default cyan
            lightController.setColors(BatteryColors.NeonCyanNormal, BatteryColors.IrisBlue, 1.0f)
            lightController.stopPulseAnimation()
            return
        }

        if (info.isCharging) {
            handleChargingState(info)
        } else {
            handleDischargingState(info)
        }

        lastWasCharging = info.isCharging
        lastWasFull = info.isFull
    }

    private fun handleChargingState(info: BatteryInfo) {
        val chargingAnimEnabled = settingsRepository.chargingAnimation.value
        val fullChargeAnimEnabled = settingsRepository.fullChargeAnimation.value

        if (info.isFull) {
            // Fully charged at 100% while charging
            // Cyan + subtle green accent: #38E07B
            lightController.setColors(
                neon = BatteryColors.GreenAccent,
                iris = BatteryColors.NeonCyanNormal,
                glow = 1.0f
            )

            // Play one short celebration pulse when reaching full charge
            if (fullChargeAnimEnabled && !lastWasFull) {
                lightController.triggerCelebrationPulse()
            }
            lightController.stopPulseAnimation()
        } else {
            // Electric cyan/blue: #00E5FF
            lightController.setColors(
                neon = BatteryColors.NeonElectricCyan,
                iris = BatteryColors.IrisBlue,
                glow = 1.15f
            )

            // When phone starts charging: play short animation through 6 neon parts
            if (chargingAnimEnabled && !lastWasCharging) {
                lightController.startChargingWaveAnimation()
            } else if (!lastWasCharging) {
                // Soft animated charging pulse
                lightController.setPulseAnimation(3.2f, 0.85f, 1.4f)
            }
        }
    }

    private fun handleDischargingState(info: BatteryInfo) {
        val lowBatteryEnabled = settingsRepository.lowBatteryEffects.value
        val pct = info.percentage

        when {
            pct >= 30 -> {
                // 30% - 100% (Normal): default cyan lighting #00CCFF, Iris blue, normal glow
                lightController.setColors(
                    neon = BatteryColors.NeonCyanNormal,
                    iris = BatteryColors.IrisBlue,
                    glow = 1.0f
                )
                lightController.stopPulseAnimation()
            }

            pct in 15..29 -> {
                // 15% - 29% (Low Battery): #FFB020, medium glow, slow pulse every few seconds
                if (lowBatteryEnabled) {
                    lightController.setColors(
                        neon = BatteryColors.NeonAmberLow,
                        iris = BatteryColors.IrisBlue,
                        glow = 0.75f
                    )
                    lightController.setPulseAnimation(3.6f, 0.55f, 1.0f)
                } else {
                    lightController.setColors(BatteryColors.NeonCyanNormal, BatteryColors.IrisBlue, 1.0f)
                    lightController.stopPulseAnimation()
                }
            }

            pct in 5..14 -> {
                // 5% - 14% (Critical): #FF3B30, iris glow red, slow warning pulse
                if (lowBatteryEnabled) {
                    lightController.setColors(
                        neon = BatteryColors.NeonRedCritical,
                        iris = BatteryColors.IrisRed,
                        glow = 0.85f
                    )
                    lightController.setPulseAnimation(1.8f, 0.4f, 1.35f)
                } else {
                    lightController.setColors(BatteryColors.NeonCyanNormal, BatteryColors.IrisBlue, 1.0f)
                    lightController.stopPulseAnimation()
                }
            }

            else -> {
                // Below 5% (Very Critical): Darken most neon parts, subtle red pulse only
                if (lowBatteryEnabled) {
                    lightController.setColors(
                        neon = BatteryColors.NeonRedCritical,
                        iris = BatteryColors.IrisRed,
                        glow = 0.28f
                    )
                    lightController.setPulseAnimation(2.4f, 0.15f, 0.45f)
                } else {
                    lightController.setColors(BatteryColors.NeonCyanNormal, BatteryColors.IrisBlue, 1.0f)
                    lightController.stopPulseAnimation()
                }
            }
        }
    }
}
