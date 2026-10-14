package com.example.floatingeye.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatingeye.battery.BatteryColors
import com.example.floatingeye.battery.BatterySettingsRepository
import com.example.floatingeye.battery.DrainTier
import com.example.floatingeye.battery.PowerDrainTelemetry
import com.example.floatingeye.shortcuts.InstalledAppsRepository
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.CyanPrimaryContainer
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Ultra-futuristic Real-time Battery Drain & Power Consumption Telemetry Card.
 * Monitors mA, % per minute, Watts, and per-app battery consumption in real time.
 * OFF by default as required by user.
 */
@Composable
fun CyberPowerDrainCard(
    isEnabled: Boolean,
    telemetry: PowerDrainTelemetry,
    settingsRepository: BatterySettingsRepository,
    installedAppsRepository: InstalledAppsRepository,
    onLaunchApp: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEnabled) CyanNeonPrimary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Master Toggle (OFF by default)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isEnabled) CyanPrimaryContainer else SurfaceContainerLow,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = if (isEnabled) CyanNeonPrimary else TextTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Real-time Battery Drain Monitor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isEnabled) "Active live cybernetic energy telemetry" else "Off by default • Tap switch to activate",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) CyanNeonPrimary else TextSecondary
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { settingsRepository.setDrainMonitorEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanNeonPrimary,
                        checkedTrackColor = CyanPrimaryContainer,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = SurfaceContainerHigh
                    )
                )
            }

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 1. Futuristic Oscilloscope Waveform HUD
                    CyberOscilloscopeHUD(
                        points = telemetry.waveformPoints,
                        currentMa = telemetry.currentMa,
                        isCharging = telemetry.isCharging
                    )

                    // 2. Real-time Metrics Grid (Discharge speed, %/min, Watts, Est. Remaining)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Metric A: Discharge Rate (mA & Watts)
                        TelemetryMetricBox(
                            icon = Icons.Rounded.Bolt,
                            label = if (telemetry.isCharging) "Charge Intake" else "Live Drain Speed",
                            value = "${telemetry.currentMa} mA",
                            subValue = String.format("%.2f W", telemetry.powerWatts),
                            color = if (telemetry.isCharging) BatteryColors.NeonElectricCyan
                            else if (telemetry.currentMa > 450) BatteryColors.NeonRedCritical
                            else BatteryColors.NeonCyanNormal,
                            modifier = Modifier.weight(1f)
                        )

                        // Metric B: Burn Rate (%/min & %/hr)
                        TelemetryMetricBox(
                            icon = Icons.Rounded.Speed,
                            label = "Burn Rate",
                            value = if (telemetry.isCharging) "+0.8% / min" else String.format("%.2f%% / min", telemetry.drainPercentPerMin),
                            subValue = if (telemetry.isCharging) "Charging up" else String.format("%.1f%% / hr", telemetry.drainPercentPerHour),
                            color = if (telemetry.drainPercentPerMin > 0.35f) BatteryColors.NeonAmberLow else CyanNeonPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Metric C: Time Remaining
                        val hours = telemetry.estimatedMinutesRemaining / 60
                        val mins = telemetry.estimatedMinutesRemaining % 60
                        TelemetryMetricBox(
                            icon = Icons.Rounded.Timer,
                            label = if (telemetry.isCharging) "Until 100%" else "Time Remaining",
                            value = "${hours}h ${mins}m",
                            subValue = "${telemetry.batteryPercentage}% battery remaining",
                            color = CyanNeonPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // Metric D: Temperature & Voltage
                        TelemetryMetricBox(
                            icon = Icons.Rounded.Thermostat,
                            label = "Thermals & Volts",
                            value = String.format("%.1f °C", telemetry.temperatureCelsius),
                            subValue = String.format("%.2f Volts", telemetry.voltageVolts),
                            color = if (telemetry.temperatureCelsius > 40f) BatteryColors.NeonRedCritical else TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 3. Top Power Burning Apps (Apps ki pi boule chaj)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Top Battery Draining Apps",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Live consumption impact",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeonPrimary
                            )
                        }

                        if (telemetry.topDrainingApps.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Analyzing active background power consumption...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            telemetry.topDrainingApps.forEach { appImpact ->
                                AppDrainItemRow(
                                    appImpact = appImpact,
                                    icon = remember(appImpact.packageName) {
                                        installedAppsRepository.getAppIcon(appImpact.packageName)
                                    },
                                    onLaunch = { onLaunchApp(appImpact.packageName, appImpact.appName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cybernetic Oscilloscope Waveform showing real-time micro-fluctuations in power draw.
 */
@Composable
private fun CyberOscilloscopeHUD(
    points: List<Float>,
    currentMa: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "oscilloscope_scan")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF060A13),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Grid background lines
                val gridColor = Color(0xFF0E1A2C)
                val cols = 8
                val rows = 4
                for (i in 1..cols) {
                    val x = (w / cols) * i
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                }
                for (j in 1..rows) {
                    val y = (h / rows) * j
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }

                // Vertical animated scanline
                val scanX = w * scanOffset
                drawLine(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            CyanNeonPrimary.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(scanX, 0f),
                    end = Offset(scanX, h),
                    strokeWidth = 2.dp.toPx()
                )

                // Waveform path
                if (points.size >= 2) {
                    val path = Path()
                    val fillPath = Path()
                    val stepX = w / (points.size - 1)

                    points.forEachIndexed { index, normY ->
                        val px = index * stepX
                        // Invert normalized value so higher load peaks higher
                        val py = h - (normY * (h * 0.75f) + (h * 0.12f))
                        if (index == 0) {
                            path.moveTo(px, py)
                            fillPath.moveTo(px, h)
                            fillPath.lineTo(px, py)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevNormY = points[index - 1]
                            val prevY = h - (prevNormY * (h * 0.75f) + (h * 0.12f))
                            val cx = (prevX + px) / 2f
                            path.cubicTo(cx, prevY, cx, py, px, py)
                            fillPath.cubicTo(cx, prevY, cx, py, px, py)
                        }
                    }

                    fillPath.lineTo(w, h)
                    fillPath.close()

                    // Glow fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                (if (isCharging) BatteryColors.NeonElectricCyan else CyanNeonPrimary).copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )

                    // Waveform line
                    drawPath(
                        path = path,
                        color = if (isCharging) BatteryColors.NeonElectricCyan else CyanNeonPrimary,
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // HUD Overlays
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isCharging) BatteryColors.NeonElectricCyan else Color(0xFF38E07B))
                    )
                    Text(
                        text = if (isCharging) "CHARGING TELEMETRY" else "LIVE DRAIN WAVEFORM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeonPrimary,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "${currentMa} mA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Metric item box for HUD telemetry.
 */
@Composable
private fun TelemetryMetricBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subValue: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

/**
 * Row item for top power draining apps.
 */
@Composable
private fun AppDrainItemRow(
    appImpact: com.example.floatingeye.battery.AppDrainImpact,
    icon: android.graphics.drawable.Drawable?,
    onLaunch: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App Icon
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceContainerHigh,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (icon != null) {
                        com.example.floatingeye.shortcuts.DrawableAppIcon(
                            drawable = icon,
                            contentDescription = appImpact.appName,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Power,
                            contentDescription = null,
                            tint = CyanNeonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // App details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appImpact.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${appImpact.drainSpeedMa} mA • ~${String.format("%.1f", appImpact.hourlyDrainPercent)}%/hr",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Cyber Badge (HIGH / MODERATE / OPTIMIZED)
            val badgeColor = Color(appImpact.powerTier.colorHex)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
            ) {
                Text(
                    text = appImpact.powerTier.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
