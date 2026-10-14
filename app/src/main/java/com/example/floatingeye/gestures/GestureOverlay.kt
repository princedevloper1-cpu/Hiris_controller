package com.example.floatingeye.gestures

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.SurfaceContainerHigh

/**
 * Full-screen transparent canvas rendering the drawn gesture line in cyan with a subtle neon glow
 * and rounded stroke ends.
 */
@Composable
fun GestureDrawingCanvas(
    captureController: GestureCaptureController,
    onPointerMove: (Float, Float) -> Unit,
    onGestureComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val points by captureController.points.collectAsStateWithLifecycle()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    captureController.startCapture()
                    captureController.addPoint(down.position.x, down.position.y)
                    onPointerMove(down.position.x, down.position.y)

                    do {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (pointer.pressed) {
                            pointer.consume()
                            captureController.addPoint(pointer.position.x, pointer.position.y)
                            onPointerMove(pointer.position.x, pointer.position.y)
                        }
                    } while (event.changes.any { it.pressed })

                    val captured = captureController.points.value
                    if (captured.size >= 4) {
                        onGestureComplete()
                    } else {
                        captureController.clear()
                    }
                }
            }
    ) {
        if (points.size >= 2) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }

            // Layer 1: Subtle outer neon glow (10dp width, transparent cyan)
            drawPath(
                path = path,
                color = CyanNeonPrimary.copy(alpha = 0.28f),
                style = Stroke(
                    width = 10.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Layer 2: Core cyan gesture stroke (4dp width, rounded caps)
            drawPath(
                path = path,
                color = CyanNeonPrimary,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        } else if (points.size == 1) {
            val p = points.first()
            drawCircle(
                color = CyanNeonPrimary,
                radius = 4.dp.toPx(),
                center = Offset(p.x, p.y)
            )
        }
    }
}

/**
 * Clean thumbnail preview of a gesture trajectory in a rounded container.
 */
@Composable
fun GestureThumbnail(
    normalizedPoints: List<GesturePoint>,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.3f)),
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (normalizedPoints.size >= 2) {
                val padding = 8.dp.toPx()
                val drawW = this.size.width - padding * 2
                val drawH = this.size.height - padding * 2

                val path = Path().apply {
                    val first = normalizedPoints.first()
                    moveTo(padding + first.x * drawW, padding + first.y * drawH)

                    for (i in 1 until normalizedPoints.size) {
                        val p = normalizedPoints[i]
                        lineTo(padding + p.x * drawW, padding + p.y * drawH)
                    }
                }

                // Glow
                drawPath(
                    path = path,
                    color = CyanNeonPrimary.copy(alpha = 0.35f),
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Line
                drawPath(
                    path = path,
                    color = CyanNeonPrimary,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
