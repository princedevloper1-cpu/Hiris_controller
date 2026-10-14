package com.example.floatingeye.controller

import com.example.floatingeye.model.FloatingEyeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.sin

/**
 * Main state and logic controller for the Floating Eye.
 * Manages screen position, gaze deflection angles, tap state, and tracking math.
 */
class FloatingEyeController(
    val config: FloatingEyeConfig,
    val lightController: FloatingEyeLightController
) {
    // Floating Eye screen coordinates in pixels
    private val _positionX = MutableStateFlow(0f)
    val positionX: StateFlow<Float> = _positionX.asStateFlow()

    private val _positionY = MutableStateFlow(0f)
    val positionY: StateFlow<Float> = _positionY.asStateFlow()

    // Eye size on screen in pixels
    var eyeWidthPx: Float = 0f
    var eyeHeightPx: Float = 0f

    // Current smoothed gaze angles in degrees
    private val _currentPitch = MutableStateFlow(0f)
    val currentPitch: StateFlow<Float> = _currentPitch.asStateFlow()

    private val _currentYaw = MutableStateFlow(0f)
    val currentYaw: StateFlow<Float> = _currentYaw.asStateFlow()

    // Target gaze angles
    private var targetPitch: Float = 0f
    private var targetYaw: Float = 0f

    // Tracking state
    private val _isTrackingTouch = MutableStateFlow(false)
    val isTrackingTouch: StateFlow<Boolean> = _isTrackingTouch.asStateFlow()

    private val _touchPoint = MutableStateFlow<Pair<Float, Float>?>(null)
    val touchPoint: StateFlow<Pair<Float, Float>?> = _touchPoint.asStateFlow()

    // Tap and interaction feedback
    private val _tapCount = MutableStateFlow(0)
    val tapCount: StateFlow<Int> = _tapCount.asStateFlow()

    private val _isPupilReacting = MutableStateFlow(false)
    val isPupilReacting: StateFlow<Boolean> = _isPupilReacting.asStateFlow()

    private var pupilReactionTimer: Float = 0f
    private var idleTime: Float = 0f

    // Track initialization
    private var isInitialized = false

    fun initializePosition(x: Float, y: Float) {
        if (!isInitialized) {
            _positionX.value = x
            _positionY.value = y
            isInitialized = true
        }
    }

    fun updatePosition(newX: Float, newY: Float) {
        _positionX.value = newX
        _positionY.value = newY
    }

    fun updateScreenDimensions(width: Float, height: Float) {
        eyeWidthPx = width
        eyeHeightPx = height
    }

    /**
     * Called when the user touches any position on screen to direct gaze.
     */
    fun onTouchGaze(touchX: Float, touchY: Float) {
        _isTrackingTouch.value = true
        _touchPoint.value = Pair(touchX, touchY)

        val eyeCenterX = _positionX.value + eyeWidthPx / 2f
        val eyeCenterY = _positionY.value + eyeHeightPx / 2f

        val dx = touchX - eyeCenterX
        val dy = touchY - eyeCenterY
        val virtualDepthPx = 480f

        // Yaw: horizontal looking angle (- = left, + = right)
        val calculatedYaw = Math.toDegrees(atan2(dx.toDouble(), virtualDepthPx.toDouble())).toFloat()
        // Pitch: vertical looking angle (- = down, + = up)
        val calculatedPitch = Math.toDegrees(atan2(-dy.toDouble(), virtualDepthPx.toDouble())).toFloat()

        targetYaw = calculatedYaw.coerceIn(-config.maxDeflectionAngleYDeg, config.maxDeflectionAngleYDeg)
        targetPitch = calculatedPitch.coerceIn(-config.maxDeflectionAngleXDeg, config.maxDeflectionAngleXDeg)
    }

    /**
     * Called when touch ends: return gaze smoothly to neutral or subtle idle saccade.
     */
    fun onTouchGazeReleased() {
        _isTrackingTouch.value = false
        _touchPoint.value = null
        targetYaw = 0f
        targetPitch = 0f
    }

    /**
     * Called when the Floating Eye itself is tapped.
     */
    fun onEyeTapped() {
        _tapCount.value += 1
        _isPupilReacting.value = true
        pupilReactionTimer = 0.35f
        lightController.triggerCyanPulse()
    }

    /**
     * Center eye position on screen.
     */
    fun centerEye(screenWidth: Float, screenHeight: Float) {
        val targetX = (screenWidth - eyeWidthPx) / 2f
        val targetY = (screenHeight - eyeHeightPx) * 0.32f
        _positionX.value = targetX
        _positionY.value = targetY
        onTouchGazeReleased()
    }

    /**
     * Frame tick for smooth interpolation of tracking nodes and effects.
     */
    fun onFrame(deltaSeconds: Float) {
        val dt = deltaSeconds.coerceIn(0.001f, 0.1f)
        lightController.updateFrame(dt)

        if (_isTrackingTouch.value) {
            idleTime = 0f
        } else {
            idleTime += dt
            // Subtle lifelike organic drift when idle
            val subtleYaw = sin(idleTime * 1.2f) * 2.5f
            val subtlePitch = sin(idleTime * 0.8f) * 1.5f
            targetYaw = subtleYaw
            targetPitch = subtlePitch
        }

        // Smooth exponential interpolation
        val speed = config.trackingSpeed
        val factor = (speed * dt).coerceIn(0f, 1f)

        _currentYaw.value += (targetYaw - _currentYaw.value) * factor
        _currentPitch.value += (targetPitch - _currentPitch.value) * factor

        // Handle pupil reaction timer
        if (_isPupilReacting.value) {
            pupilReactionTimer -= dt
            if (pupilReactionTimer <= 0f) {
                _isPupilReacting.value = false
            }
        }
    }
}
