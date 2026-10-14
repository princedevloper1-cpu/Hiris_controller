package com.example.floatingeye.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Executive Aerospace Sensor Core Illustration.
 * 
 * Clean, professional, precision-machined CAD/aerospace optic aesthetic:
 * - CNC Titanium dial with satin dark PVD finish.
 * - Micro-machined calibrated tick marks.
 * - Multi-coated sapphire lens with realistic optical reflection.
 * - Precision reticle crosshair.
 */
@Composable
fun HirisOrbIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    animated: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "OrbTransition")

    val orbitRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitRotation"
    )

    val pulseGlow by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = this.size.width
            val height = this.size.height
            val center = Offset(width * 0.5f, height * 0.5f)
            val outerRadius = width * 0.42f

            // 1. Ambient Drop Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.65f),
                radius = outerRadius * 1.02f,
                center = Offset(center.x, center.y + outerRadius * 0.05f)
            )

            // 2. CNC Titanium Bezel
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF283344),
                        Color(0xFF141B26),
                        Color(0xFF38465B),
                        Color(0xFF161E2B),
                        Color(0xFF2E3B4E),
                        Color(0xFF141B26),
                        Color(0xFF283344)
                    ),
                    center = center
                ),
                radius = outerRadius,
                center = center
            )

            // Metallic Bevel Highlight
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF7086A6).copy(alpha = 0.75f),
                        Color(0xFF1B2433).copy(alpha = 0.25f),
                        Color(0xFF5E7391).copy(alpha = 0.6f)
                    ),
                    start = Offset(center.x - outerRadius, center.y - outerRadius),
                    end = Offset(center.x + outerRadius, center.y + outerRadius)
                ),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.4f)
            )

            // 3. Recessed Obsidian Dial & Carbon Well
            val dialRadius = outerRadius * 0.88f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF111827),
                        Color(0xFF090D15),
                        Color(0xFF03060B)
                    ),
                    center = center,
                    radius = dialRadius
                ),
                radius = dialRadius,
                center = center
            )

            // 4. Micro-Indexing Tachymeter Ticks
            val tickRadius = dialRadius * 0.94f
            val tickLength = dialRadius * 0.08f
            val numTicks = 24
            rotate(degrees = orbitRotation, pivot = center) {
                for (i in 0 until numTicks) {
                    val angle = (i * 360f / numTicks)
                    val rad = Math.toRadians(angle.toDouble())
                    val isCardinal = (i % 6 == 0)

                    val startX = center.x + ((tickRadius - (if (isCardinal) tickLength * 1.3f else tickLength)) * cos(rad)).toFloat()
                    val startY = center.y + ((tickRadius - (if (isCardinal) tickLength * 1.3f else tickLength)) * sin(rad)).toFloat()
                    val endX = center.x + (tickRadius * cos(rad)).toFloat()
                    val endY = center.y + (tickRadius * sin(rad)).toFloat()

                    drawLine(
                        color = if (isCardinal) Color(0xFF00E5FF).copy(alpha = 0.7f * pulseGlow) else Color(0xFF4B5563).copy(alpha = 0.4f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isCardinal) 1.5f else 0.9f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 5. Precision Sapphire Optical Element
            val lensRadius = dialRadius * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F1E33),
                        Color(0xFF081220),
                        Color(0xFF03070E)
                    ),
                    center = center,
                    radius = lensRadius
                ),
                radius = lensRadius,
                center = center
            )

            // Stepper ring
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.65f * pulseGlow),
                radius = lensRadius * 0.86f,
                center = center,
                style = Stroke(width = 1.4f)
            )

            // Central reticle aperture
            val reticleRadius = lensRadius * 0.42f
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(center.x - reticleRadius, center.y),
                end = Offset(center.x + reticleRadius, center.y),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(center.x, center.y - reticleRadius),
                end = Offset(center.x, center.y + reticleRadius),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )

            // Pinpoint jewel dot
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 2.0f,
                center = center
            )

            // 6. Anti-Reflective Optical Coating Sheen
            val sheenPath = Path().apply {
                val sheenRadius = outerRadius * 0.85f
                val sheenTop = center.y - sheenRadius
                val sheenBottom = center.y - sheenRadius * 0.2f
                moveTo(center.x - sheenRadius * 0.6f, sheenTop + sheenRadius * 0.2f)
                cubicTo(
                    center.x - sheenRadius * 0.3f, sheenTop,
                    center.x + sheenRadius * 0.3f, sheenTop,
                    center.x + sheenRadius * 0.6f, sheenTop + sheenRadius * 0.2f
                )
                cubicTo(
                    center.x + sheenRadius * 0.2f, sheenBottom,
                    center.x - sheenRadius * 0.2f, sheenBottom,
                    center.x - sheenRadius * 0.6f, sheenTop + sheenRadius * 0.2f
                )
                close()
            }
            drawPath(
                path = sheenPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    start = Offset(center.x, center.y - outerRadius),
                    end = Offset(center.x, center.y)
                )
            )
        }
    }
}
