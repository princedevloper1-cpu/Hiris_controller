package com.example.floatingeye.gestures

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot

class GestureCaptureController {
    private val _points = MutableStateFlow<List<GesturePoint>>(emptyList())
    val points: StateFlow<List<GesturePoint>> = _points.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    fun startCapture() {
        _points.value = emptyList()
        _isCapturing.value = true
    }

    fun addPoint(x: Float, y: Float) {
        if (!_isCapturing.value) return

        val currentList = _points.value
        if (currentList.isNotEmpty()) {
            val last = currentList.last()
            val dist = hypot(x - last.x, y - last.y)
            // Filter noise / micro-jitter
            if (dist < 3f) return
        }

        _points.value = currentList + GesturePoint(x, y)
    }

    fun finishCapture(): List<GesturePoint> {
        _isCapturing.value = false
        return _points.value
    }

    fun clear() {
        _points.value = emptyList()
        _isCapturing.value = false
    }
}
