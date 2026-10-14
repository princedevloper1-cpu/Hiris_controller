package com.example.floatingeye.battery

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatterySettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _reactionsEnabled = MutableStateFlow(prefs.getBoolean(KEY_REACTIONS_ENABLED, false))
    val reactionsEnabled: StateFlow<Boolean> = _reactionsEnabled.asStateFlow()

    private val _lowBatteryEffects = MutableStateFlow(prefs.getBoolean(KEY_LOW_BATTERY_EFFECTS, true))
    val lowBatteryEffects: StateFlow<Boolean> = _lowBatteryEffects.asStateFlow()

    private val _chargingAnimation = MutableStateFlow(prefs.getBoolean(KEY_CHARGING_ANIM, true))
    val chargingAnimation: StateFlow<Boolean> = _chargingAnimation.asStateFlow()

    private val _fullChargeAnimation = MutableStateFlow(prefs.getBoolean(KEY_FULL_CHARGE_ANIM, true))
    val fullChargeAnimation: StateFlow<Boolean> = _fullChargeAnimation.asStateFlow()

    // Realtime Battery Drain & Power Consumption Monitor (Off by default as required by user)
    private val _drainMonitorEnabled = MutableStateFlow(prefs.getBoolean(KEY_DRAIN_MONITOR_ENABLED, false))
    val drainMonitorEnabled: StateFlow<Boolean> = _drainMonitorEnabled.asStateFlow()

    // Optional floating overlay HUD telemetry badge (Off by default)
    private val _showDrainHudOnOverlay = MutableStateFlow(prefs.getBoolean(KEY_SHOW_DRAIN_HUD_ON_OVERLAY, false))
    val showDrainHudOnOverlay: StateFlow<Boolean> = _showDrainHudOnOverlay.asStateFlow()

    fun setReactionsEnabled(enabled: Boolean) {
        _reactionsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_REACTIONS_ENABLED, enabled).apply()
    }

    fun setLowBatteryEffects(enabled: Boolean) {
        _lowBatteryEffects.value = enabled
        prefs.edit().putBoolean(KEY_LOW_BATTERY_EFFECTS, enabled).apply()
    }

    fun setChargingAnimation(enabled: Boolean) {
        _chargingAnimation.value = enabled
        prefs.edit().putBoolean(KEY_CHARGING_ANIM, enabled).apply()
    }

    fun setFullChargeAnimation(enabled: Boolean) {
        _fullChargeAnimation.value = enabled
        prefs.edit().putBoolean(KEY_FULL_CHARGE_ANIM, enabled).apply()
    }

    fun setDrainMonitorEnabled(enabled: Boolean) {
        _drainMonitorEnabled.value = enabled
        prefs.edit().putBoolean(KEY_DRAIN_MONITOR_ENABLED, enabled).apply()
    }

    fun setShowDrainHudOnOverlay(enabled: Boolean) {
        _showDrainHudOnOverlay.value = enabled
        prefs.edit().putBoolean(KEY_SHOW_DRAIN_HUD_ON_OVERLAY, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "floating_eye_battery_prefs"
        private const val KEY_REACTIONS_ENABLED = "battery_reactions_enabled"
        private const val KEY_LOW_BATTERY_EFFECTS = "low_battery_effects_enabled"
        private const val KEY_CHARGING_ANIM = "charging_animation_enabled"
        private const val KEY_FULL_CHARGE_ANIM = "full_charge_animation_enabled"
        private const val KEY_DRAIN_MONITOR_ENABLED = "drain_monitor_enabled"
        private const val KEY_SHOW_DRAIN_HUD_ON_OVERLAY = "show_drain_hud_on_overlay"
    }
}
