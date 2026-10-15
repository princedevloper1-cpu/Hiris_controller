package com.example.floatingeye.controller

import androidx.compose.ui.graphics.Color
import com.example.floatingeye.battery.BatteryColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

/**
 * Controls lighting, bloom post-processing, luminous emissive effects,
 * and battery-driven color and pulse transitions.
 */
class FloatingEyeLightController(
    initialBloomEnabled: Boolean = true,
    initialBloomStrength: Float = 0.35f
) {
    private val _bloomEnabled = MutableStateFlow(initialBloomEnabled)
    val bloomEnabled: StateFlow<Boolean> = _bloomEnabled.asStateFlow()

    private val _bloomStrength = MutableStateFlow(initialBloomStrength)
    val bloomStrength: StateFlow<Float> = _bloomStrength.asStateFlow()

    private val _emissivePulse = MutableStateFlow(1.0f)
    val emissivePulse: StateFlow<Float> = _emissivePulse.asStateFlow()

    // Battery Reactions Properties
    private val _neonColor = MutableStateFlow(BatteryColors.NeonCyanNormal)
    val neonColor: StateFlow<Color> = _neonColor.asStateFlow()

    private val _irisColor = MutableStateFlow(BatteryColors.IrisBlue)
    val irisColor: StateFlow<Color> = _irisColor.asStateFlow()

    private val _glowIntensity = MutableStateFlow(1.0f)
    val glowIntensity: StateFlow<Float> = _glowIntensity.asStateFlow()

    // Battery Pulse Animation: period and min/max intensity
    private var pulsePeriod = 0f
    private var pulseMin = 1.0f
    private var pulseMax = 1.0f
    private var pulseTimer = 0f

    // Charging Sequential Wave through 6 neon parts:
    // 0: 04_Neon_Outer_Ring
    // 1: 07_Iris_Cyan_Ring
    // 2: 14_Left_Side_Neon
    // 3: 15_Right_Side_Neon
    // 4: 16_Top_Neon_Strip
    // 5: 17_Bottom_Neon_Strip
    private val _chargingWavePartIndex = MutableStateFlow(-1)
    val chargingWavePartIndex: StateFlow<Int> = _chargingWavePartIndex.asStateFlow()

    private var chargingWaveTimer = 0f
    private var isPlayingChargingWave = false

    // Celebration pulse for fully charged state
    private var celebrationTimer = 0f

    fun setBloomEnabled(enabled: Boolean, filamentView: Any? = null) {
        _bloomEnabled.value = enabled
    }

    fun setBloomStrength(strength: Float, filamentView: Any? = null) {
        _bloomStrength.value = strength.coerceIn(0f, 1f)
    }

    fun setGlowIntensity(intensity: Float) {
        _glowIntensity.value = intensity.coerceIn(0.5f, 2.5f)
    }

    fun triggerCyanPulse() {
        _emissivePulse.value = 1.75f
    }

    fun setColors(neon: Color, iris: Color, glow: Float) {
        _neonColor.value = neon
        _irisColor.value = iris
        _glowIntensity.value = glow.coerceIn(0f, 2f)
    }

    fun setPulseAnimation(periodSeconds: Float, min: Float, max: Float) {
        pulsePeriod = periodSeconds
        pulseMin = min
        pulseMax = max
        pulseTimer = 0f
    }

    fun stopPulseAnimation() {
        pulsePeriod = 0f
        pulseMin = 1.0f
        pulseMax = 1.0f
        _emissivePulse.value = 1.0f
    }

    fun startChargingWaveAnimation() {
        isPlayingChargingWave = true
        chargingWaveTimer = 0f
        _chargingWavePartIndex.value = 0
    }

    fun triggerCelebrationPulse() {
        celebrationTimer = 1.8f
        _emissivePulse.value = 1.85f
    }

    fun updateFrame(deltaSeconds: Float) {
        // Tap / Celebration decay
        if (celebrationTimer > 0f) {
            celebrationTimer = (celebrationTimer - deltaSeconds).coerceAtLeast(0f)
            _emissivePulse.value = (1.0f + celebrationTimer * 0.45f)
        } else if (_emissivePulse.value > 1.0f && pulsePeriod <= 0f) {
            _emissivePulse.value = (_emissivePulse.value - deltaSeconds * 2.5f).coerceAtLeast(1.0f)
        }

        // Charging wave sequence through 6 nodes
        if (isPlayingChargingWave) {
            chargingWaveTimer += deltaSeconds
            val stepTime = 0.22f // 220ms per part
            val totalParts = 6
            val currentPart = (chargingWaveTimer / stepTime).toInt()

            if (currentPart < totalParts) {
                _chargingWavePartIndex.value = currentPart
            } else {
                isPlayingChargingWave = false
                _chargingWavePartIndex.value = -1
                // Transition into calm breathing charging pulse (period = 3.2s)
                setPulseAnimation(3.2f, 0.85f, 1.4f)
            }
        }

        // Continuous pulse wave (e.g. low battery, critical warning, charging breathing)
        if (pulsePeriod > 0f && celebrationTimer <= 0f) {
            pulseTimer += deltaSeconds
            val progress = (pulseTimer % pulsePeriod) / pulsePeriod
            // Sine wave between min and max
            val wave = (sin(progress * 2.0 * Math.PI).toFloat() + 1f) / 2f
            _emissivePulse.value = pulseMin + wave * (pulseMax - pulseMin)
        }
    }

    fun applyToFilamentView(view: Any? = null) {
        // Filament view hook
    }
}
