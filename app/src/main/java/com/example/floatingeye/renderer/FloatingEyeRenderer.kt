package com.example.floatingeye.renderer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.controller.FloatingEyeController
import com.example.floatingeye.controller.FloatingEyeLightController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance Executive Ocular Sensor & Floating Core Renderer.
 * 
 * Replaces cartoonish eye aesthetics with a luxury aerospace telemetry lens:
 * - CNC Titanium / Graphite matte outer housing.
 * - Micro-machined calibrated optical aperture with multi-coated anti-reflective sapphire.
 * - Responsive 3D physical deflection tracking with smooth gimbal damper physics.
 * - Precision concentric reticle indexing without exaggerated anime/cartoon pupil effects.
 * - Realistic camera lens / satellite optic reflection highlights.
 */
@Composable
fun FloatingEyeRenderer(
    controller: FloatingEyeController,
    lightController: FloatingEyeLightController,
    modifier: Modifier = Modifier
) {
    val pitch by controller.currentPitch.collectAsStateWithLifecycle()
    val yaw by controller.currentYaw.collectAsStateWithLifecycle()
    val isPupilReacting by controller.isPupilReacting.collectAsStateWithLifecycle()

    val neonColor by lightController.neonColor.collectAsStateWithLifecycle()
    val irisColor by lightController.irisColor.collectAsStateWithLifecycle()
    val emissivePulse by lightController.emissivePulse.collectAsStateWithLifecycle()
    val glowIntensity by lightController.glowIntensity.collectAsStateWithLifecycle()

    // 60fps frame tick loop to update smoothing physics in controller
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (isActive) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastTime = now
            controller.onFrame(dt)
            lightController.updateFrame(dt)
            delay(16L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "eye_pro_fx")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rot"
    )

    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.minDimension / 2f) * 0.88f
            val maxDeflectionX = outerRadius * 0.22f
            val maxDeflectionY = outerRadius * 0.22f

            // Gaze deflection offset (subtle, physical gimbal movement)
            val gazeDeflectionX = (yaw / 34f).coerceIn(-1f, 1f) * maxDeflectionX
            val gazeDeflectionY = (-pitch / 28f).coerceIn(-1f, 1f) * maxDeflectionY
            val gazeCenter = Offset(center.x + gazeDeflectionX, center.y + gazeDeflectionY)

            val effectivePulse = emissivePulse * glowIntensity * ambientPulse

            // 1. Deep Backing Drop Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.7f),
                radius = outerRadius * 1.03f,
                center = Offset(center.x, center.y + outerRadius * 0.05f)
            )

            // 2. CNC Titanium Bezel (High-End Brushed Finish)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF2B374A),
                        Color(0xFF141C27),
                        Color(0xFF3B4B63),
                        Color(0xFF17202D),
                        Color(0xFF303F54),
                        Color(0xFF141C27),
                        Color(0xFF2B374A)
                    ),
                    center = center
                ),
                radius = outerRadius,
                center = center
            )

            // Beveled razor highlight ring
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF7F95B3).copy(alpha = 0.65f),
                        Color(0xFF1E2838).copy(alpha = 0.2f),
                        Color(0xFF647B99).copy(alpha = 0.5f)
                    ),
                    start = Offset(center.x - outerRadius, center.y - outerRadius),
                    end = Offset(center.x + outerRadius, center.y + outerRadius)
                ),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.4f)
            )

            // 3. Recessed Optical Chassis (Deep Obsidian)
            val chassisRadius = outerRadius * 0.88f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF111827),
                        Color(0xFF090D15),
                        Color(0xFF030509)
                    ),
                    center = center,
                    radius = chassisRadius
                ),
                radius = chassisRadius,
                center = center
            )

            // Micro-engraved calibration marks (Tachymeter dial)
            val numEngravings = 24
            val tickRadius = chassisRadius * 0.95f
            val tickLen = chassisRadius * 0.07f
            rotate(degrees = orbitAngle, pivot = center) {
                for (i in 0 until numEngravings) {
                    val angleDeg = i * 360f / numEngravings
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val isMajor = (i % 6 == 0)

                    val startX = center.x + ((tickRadius - (if (isMajor) tickLen * 1.4f else tickLen)) * cos(rad)).toFloat()
                    val startY = center.y + ((tickRadius - (if (isMajor) tickLen * 1.4f else tickLen)) * sin(rad)).toFloat()
                    val endX = center.x + (tickRadius * cos(rad)).toFloat()
                    val endY = center.y + (tickRadius * sin(rad)).toFloat()

                    drawLine(
                        color = if (isMajor) neonColor.copy(alpha = 0.7f * effectivePulse) else Color(0xFF4B5563).copy(alpha = 0.45f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isMajor) 1.5f else 0.9f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Multi-Coated Sapphire Optical Lens Barrel
            val lensRadius = chassisRadius * 0.76f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F1E33),
                        Color(0xFF081220),
                        Color(0xFF03070E)
                    ),
                    center = gazeCenter,
                    radius = lensRadius
                ),
                radius = lensRadius,
                center = gazeCenter
            )

            // Precision Iris Blade Stepper Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        neonColor.copy(alpha = 0.5f * effectivePulse),
                        irisColor.copy(alpha = 0.8f * effectivePulse),
                        Color(0xFF00E5FF).copy(alpha = 0.9f * effectivePulse),
                        neonColor.copy(alpha = 0.5f * effectivePulse)
                    ),
                    center = gazeCenter
                ),
                radius = lensRadius * 0.84f,
                center = gazeCenter,
                style = Stroke(width = 1.8f)
            )

            // 5. Central High-Precision Optical Sensor / Aperture (Camera Core)
            val pupilBaseScale = if (isPupilReacting) 0.88f else 1.0f
            val apertureRadius = lensRadius * 0.45f * pupilBaseScale

            // Sensor well
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF020408),
                        Color(0xFF000204)
                    ),
                    center = gazeCenter,
                    radius = apertureRadius
                ),
                radius = apertureRadius,
                center = gazeCenter
            )

            // High-precision reticle crosshair inside sensor
            val crosshairLen = apertureRadius * 0.75f
            drawLine(
                color = neonColor.copy(alpha = 0.6f * effectivePulse),
                start = Offset(gazeCenter.x - crosshairLen, gazeCenter.y),
                end = Offset(gazeCenter.x + crosshairLen, gazeCenter.y),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = neonColor.copy(alpha = 0.6f * effectivePulse),
                start = Offset(gazeCenter.x, gazeCenter.y - crosshairLen),
                end = Offset(gazeCenter.x, gazeCenter.y + crosshairLen),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round
            )

            // Micro optical jewel dot at center
            drawCircle(
                color = Color.White,
                radius = 1.4f,
                center = gazeCenter
            )

            // 6. Anti-Reflective Optical Coating (Curved lens highlight sheen)
            val sheenPath = Path().apply {
                val sheenRadius = outerRadius * 0.80f
                val sheenTop = center.y - sheenRadius
                val sheenBottom = center.y - sheenRadius * 0.25f
                moveTo(center.x - sheenRadius * 0.55f, sheenTop + sheenRadius * 0.18f)
                cubicTo(
                    center.x - sheenRadius * 0.25f, sheenTop,
                    center.x + sheenRadius * 0.25f, sheenTop,
                    center.x + sheenRadius * 0.55f, sheenTop + sheenRadius * 0.18f
                )
                cubicTo(
                    center.x + sheenRadius * 0.2f, sheenBottom,
                    center.x - sheenRadius * 0.2f, sheenBottom,
                    center.x - sheenRadius * 0.55f, sheenTop + sheenRadius * 0.18f
                )
                close()
            }
            drawPath(
                path = sheenPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    start = Offset(center.x, center.y - outerRadius),
                    end = Offset(center.x, center.y)
                )
            )

            // Pinpoint luxury specular glint
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = 1.5f,
                center = Offset(center.x - outerRadius * 0.35f, center.y - outerRadius * 0.35f)
            )
        }
    }
}
