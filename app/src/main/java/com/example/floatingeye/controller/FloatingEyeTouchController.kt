package com.example.floatingeye.controller

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.hypot

/**
 * Handles touch gestures for the Floating Eye.
 * Explicitly differentiates tap from drag, keeps eye where released,
 * and captures screen gaze touch positions.
 */
class FloatingEyeTouchController(
    private val controller: FloatingEyeController
) {
    /**
     * Modifier applied to the Floating Eye container to handle drag and tap.
     */
    fun createFloatingEyeTouchModifier(
        screenWidthPx: Float,
        screenHeightPx: Float,
        eyeSizePx: Float
    ): Modifier = Modifier.pointerInput(Unit) {
        val dragThreshold = controller.config.dragThresholdPx
        val tapMaxDuration = controller.config.tapMaxDurationMs

        awaitEachGesture {
            val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
            val startTime = System.currentTimeMillis()
            val startTouchPos = down.position
            var totalMovedDistance = 0f
            var isDrag = false

            var initialEyeX = controller.positionX.value
            var initialEyeY = controller.positionY.value

            do {
                val event = awaitPointerEvent()
                val currentChange = event.changes.firstOrNull { it.id == down.id } ?: break

                if (currentChange.pressed) {
                    val delta = currentChange.position - startTouchPos
                    totalMovedDistance = hypot(delta.x, delta.y)

                    if (!isDrag && totalMovedDistance > dragThreshold) {
                        isDrag = true
                    }

                    if (isDrag) {
                        currentChange.consume()
                        val rawNewX = initialEyeX + delta.x
                        val rawNewY = initialEyeY + delta.y

                        // Clamp within screen boundaries
                        val minX = 0f
                        val maxX = (screenWidthPx - eyeSizePx).coerceAtLeast(0f)
                        val minY = 0f
                        val maxY = (screenHeightPx - eyeSizePx).coerceAtLeast(0f)

                        controller.updatePosition(
                            newX = rawNewX.coerceIn(minX, maxX),
                            newY = rawNewY.coerceIn(minY, maxY)
                        )
                    }
                } else {
                    // Finger released
                    val duration = System.currentTimeMillis() - startTime
                    if (!isDrag && totalMovedDistance <= dragThreshold && duration <= tapMaxDuration) {
                        controller.onEyeTapped()
                    }
                    // When released after dragging, it stays exactly where placed (no snap back)
                    break
                }
            } while (event.changes.any { it.pressed })
        }
    }

    /**
     * Modifier applied to the workspace background to track touches for iris gaze deflection.
     */
    fun createGazeWorkspaceModifier(): Modifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            controller.onTouchGaze(down.position.x, down.position.y)

            do {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.pressed) {
                    controller.onTouchGaze(change.position.x, change.position.y)
                } else {
                    controller.onTouchGazeReleased()
                    break
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
