package com.example.floatingeye.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanNeonPrimary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ultra-Premium Executive Floating Companion Icon (HIRIS Pro Core).
 * 
 * Re-engineered from scratch to deliver a high-end, professional hardware look:
 * - CNC Titanium / Obsidian bezel with subtle chamfered brushed metallic highlights.
 * - Precision tachymeter / micro-indexing ring with laser-etched tick marks.
 * - Multi-layer sapphire lens depth with realistic caustic specular reflections.
 * - High-precision telemetry arc (smooth battery / system state gauge).
 * - Central precision crosshair / multi-tool aperture (transforms smoothly between [+] and [x]).
 * - ZERO cartoonish/gimmicky AI elements: sleek, clean, executive aerospace aesthetic.
 */
@Composable
fun FloatingEyePlusWidget(
    size: Dp = 64.dp,
    isOpen: Boolean = false,
    isCharging: Boolean = false,
    isFull: Boolean = false,
    batteryPercentage: Int = 100,
    neonColor: Color = CyanNeonPrimary,
    irisColor: Color = Color(0xFF00B0FF),
    emissivePulse: Float = 1.0f,
    glowIntensity: Float = 1.0f,
    bloomStrength: Float = 0.35f,
    modifier: Modifier = Modifier
) {
    // Rotation transition for + to x transform with high-precision mechanical spring
    val rotationAngle by animateFloatAsState(
        targetValue = if (isOpen) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "core_rotation"
    )

    // Smooth mechanical breathing / pulsing
    val infiniteTransition = rememberInfiniteTransition(label = "pro_core_fx")

    // Precision tachymeter slow indexing rotation
    val microIndexRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "index_rot"
    )

    // Smooth charging radar sweep (controlled, subtle)
    val chargingSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "charging_sweep"
    )

    // Subtle optical breathing
    val opticalPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "optical_pulse"
    )

    // Refined, high-end status accent palette (non-garish, luxury aerospace tones)
    val statusAccent = when {
        isCharging -> Color(0xFF00E5FF) // Electric precision cyan
        isFull || batteryPercentage >= 95 -> Color(0xFF10B981) // Emerald telemetry
        batteryPercentage >= 30 -> neonColor // Primary brand cyan
        batteryPercentage >= 15 -> Color(0xFFF59E0B) // Amber warning
        else -> Color(0xFFEF4444) // Ruby critical
    }
    val bloomScale = 0.25f + bloomStrength.coerceIn(0f, 1f) * 0.75f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = this.size.width
            val height = this.size.height
            val center = Offset(width / 2f, height / 2f)
            val outerRadius = (this.size.minDimension / 2f) * 0.94f

            // -------------------------------------------------------------
            // LAYER 1: Deep Drop Shadow & Ambient Chassis Bloom
            // -------------------------------------------------------------
            drawCircle(
                color = Color.Black.copy(alpha = 0.65f),
                radius = outerRadius * 1.02f,
                center = Offset(center.x, center.y + outerRadius * 0.05f)
            )

            // Ultra-subtle backlight halo (only when active / status accent)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        statusAccent.copy(alpha = 0.16f * opticalPulse * glowIntensity * bloomScale),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius * 1.28f
                ),
                radius = outerRadius * 1.28f,
                center = center
            )

            // -------------------------------------------------------------
            // LAYER 2: CNC Machined Aerospace Bezel (Titanium & Obsidian)
            // -------------------------------------------------------------
            // Dark brushed titanium outer case
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

            // Fine chamfered ring highlight (metallic razor bevel)
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
                style = Stroke(width = 1.2.dp.toPx())
            )

            // -------------------------------------------------------------
            // LAYER 3: Recessed Obsidian Dial & Carbon Well
            // -------------------------------------------------------------
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

            // Inner dark inset shadow ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.7f),
                radius = dialRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // -------------------------------------------------------------
            // LAYER 4: Micro-Indexing Tachymeter Ticks (Laser-etched)
            // -------------------------------------------------------------
            val tickRadius = dialRadius * 0.94f
            val tickLength = dialRadius * 0.08f
            val numTicks = 24
            rotate(degrees = microIndexRotation, pivot = center) {
                for (i in 0 until numTicks) {
                    val angle = (i * 360f / numTicks)
                    val rad = Math.toRadians(angle.toDouble())
                    val isCardinal = (i % 6 == 0)

                    val startX = center.x + ((tickRadius - (if (isCardinal) tickLength * 1.3f else tickLength)) * cos(rad)).toFloat()
                    val startY = center.y + ((tickRadius - (if (isCardinal) tickLength * 1.3f else tickLength)) * sin(rad)).toFloat()
                    val endX = center.x + (tickRadius * cos(rad)).toFloat()
                    val endY = center.y + (tickRadius * sin(rad)).toFloat()

                    drawLine(
                        color = if (isCardinal) statusAccent.copy(alpha = 0.65f) else Color(0xFF4B5563).copy(alpha = 0.4f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isCardinal) 1.2.dp.toPx() else 0.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // -------------------------------------------------------------
            // LAYER 5: Precision Telemetry Battery Gauge Arc
            // -------------------------------------------------------------
            val trackRadius = dialRadius * 0.80f
            val trackStroke = dialRadius * 0.10f
            val sweepAngle = ((batteryPercentage.coerceIn(0, 100) / 100f) * 360f).coerceAtLeast(6f)
            val arcTopLeft = Offset(center.x - trackRadius, center.y - trackRadius)
            val arcSize = Size(trackRadius * 2f, trackRadius * 2f)

            // Track groove (stealth slate track)
            drawArc(
                color = Color(0xFF1F2937).copy(alpha = 0.65f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = trackStroke * 0.6f, cap = StrokeCap.Round)
            )

            // Active Telemetry Arc (Precision Crisp Line)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        statusAccent.copy(alpha = 0.4f * bloomScale),
                        statusAccent,
                        Color.White.copy(alpha = 0.95f),
                        statusAccent
                    ),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = trackStroke, cap = StrokeCap.Round)
            )

            // Precision LED Jewel Bead at Arc Tip
            val tipAngleRad = Math.toRadians((-90f + sweepAngle).toDouble())
            val tipX = center.x + (trackRadius * cos(tipAngleRad)).toFloat()
            val tipY = center.y + (trackRadius * sin(tipAngleRad)).toFloat()

            drawCircle(
                color = Color.White,
                radius = trackStroke * 0.55f,
                center = Offset(tipX, tipY)
            )
            drawCircle(
                color = statusAccent.copy(alpha = 0.5f * bloomScale),
                radius = trackStroke * 0.95f,
                center = Offset(tipX, tipY)
            )

            // Rotating charging scan beam if device is plugged in
            if (isCharging) {
                rotate(degrees = chargingSweepAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                statusAccent.copy(alpha = 0.1f * bloomScale),
                                Color.White.copy(alpha = 0.85f),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = trackStroke * 0.85f, cap = StrokeCap.Round)
                    )
                }
            }

            // -------------------------------------------------------------
            // LAYER 6: Central Core Hub (Machined Steel Button Island)
            // -------------------------------------------------------------
            val hubRadius = trackRadius * 0.68f

            // Hub base drop shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.85f),
                radius = hubRadius * 1.05f,
                center = center
            )

            // Radial gradient metallic disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF263345),
                        Color(0xFF151D2A),
                        Color(0xFF0A0F17)
                    ),
                    center = Offset(center.x - hubRadius * 0.2f, center.y - hubRadius * 0.2f),
                    radius = hubRadius
                ),
                radius = hubRadius,
                center = center
            )

            // Fine polished hub rim
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color(0xFF1E2838),
                        Color(0xFF6B7280).copy(alpha = 0.35f)
                    ),
                    start = Offset(center.x - hubRadius, center.y - hubRadius),
                    end = Offset(center.x + hubRadius, center.y + hubRadius)
                ),
                radius = hubRadius,
                center = center,
                style = Stroke(width = 1.0.dp.toPx())
            )

            // -------------------------------------------------------------
            // LAYER 7: Precision Aperture Reticle [+] / [x]
            // -------------------------------------------------------------
            rotate(degrees = rotationAngle, pivot = center) {
                drawProApertureReticle(
                    center = center,
                    reticleRadius = hubRadius * 0.62f,
                    strokeWidth = 2.4.dp.toPx(),
                    accentColor = statusAccent,
                    isOpen = isOpen
                )
            }

            // -------------------------------------------------------------
            // LAYER 8: Sapphire Crystal Glint & Specular Reflection
            // -------------------------------------------------------------
            // Ultra-refined diagonal light sheen across the top-left lens
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

            // Pinpoint luxury specular glint
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = 1.4.dp.toPx(),
                center = Offset(center.x - outerRadius * 0.38f, center.y - outerRadius * 0.38f)
            )
        }
    }
}

/**
 * Draws an executive, geometric reticle inside the hub.
 * Replaces cartoonish lines with high-precision engineering crosshairs and corner pips.
 */
private fun DrawScope.drawProApertureReticle(
    center: Offset,
    reticleRadius: Float,
    strokeWidth: Float,
    accentColor: Color,
    isOpen: Boolean
) {
    val armLen = reticleRadius
    val centerClearance = if (isOpen) 0f else reticleRadius * 0.20f

    // 1. Subtle central ambient node
    drawCircle(
        color = accentColor.copy(alpha = 0.35f),
        radius = strokeWidth * 0.9f,
        center = center
    )

    // 2. High-precision Crosshair Arms with chamfered caps
    // Horizontal Bar (Left & Right)
    drawLine(
        color = Color.White,
        start = Offset(center.x - armLen, center.y),
        end = Offset(center.x - centerClearance, center.y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White,
        start = Offset(center.x + centerClearance, center.y),
        end = Offset(center.x + armLen, center.y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Vertical Bar (Top & Bottom)
    drawLine(
        color = Color.White,
        start = Offset(center.x, center.y - armLen),
        end = Offset(center.x, center.y - centerClearance),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White,
        start = Offset(center.x, center.y + centerClearance),
        end = Offset(center.x, center.y + armLen),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // 3. Central Precision Dot
    drawCircle(
        color = Color.White,
        radius = strokeWidth * 0.55f,
        center = center
    )
}
