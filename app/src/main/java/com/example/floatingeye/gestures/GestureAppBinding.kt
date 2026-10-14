package com.example.floatingeye.gestures

import java.util.UUID

data class GesturePoint(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class GestureAppBinding(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val normalizedPoints: List<GesturePoint>,
    val rawPoints: List<GesturePoint> = emptyList(),
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
