package com.example.floatingeye.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.battery.FloatingEyeBatteryController
import com.example.floatingeye.controller.FloatingEyeController
import com.example.floatingeye.controller.FloatingEyeLightController
import com.example.floatingeye.extensions.repository.ExtensionRepository
import com.example.floatingeye.model.FloatingEyeConfig
import com.example.floatingeye.overlay.FloatingEyeOverlayService
import com.example.floatingeye.overlay.FloatingEyePositionManager
import com.example.floatingeye.shortcuts.AppShortcutRepository
import com.example.floatingeye.shortcuts.RadialAppShortcutMenu
import com.example.floatingeye.ui.FloatingEyePlusWidget
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun HomeScreen(
    positionManager: FloatingEyePositionManager,
    config: FloatingEyeConfig,
    extensionRepository: ExtensionRepository,
    batteryController: FloatingEyeBatteryController,
    shortcutRepository: AppShortcutRepository,
    onNavigateToExtensions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOverlayRunning by FloatingEyeOverlayService.isServiceRunning.collectAsStateWithLifecycle()
    val extensions by extensionRepository.extensions.collectAsStateWithLifecycle()
    val batteryInfo by batteryController.batteryInfo.collectAsStateWithLifecycle()
    val shortcuts by shortcutRepository.shortcuts.collectAsStateWithLifecycle()

    val activeCount = extensions.count { it.isInstalled && it.isActive }
    val overlaySize = positionManager.getOverlaySize()
    val isShortcutsActive = extensions.firstOrNull { it.id == ExtensionRepository.ID_SHORTCUTS }?.isActive == true
    val isBatteryActive = extensions.firstOrNull { it.id == ExtensionRepository.ID_BATTERY }?.isActive == true

    // Preview state for embedded plus widget
    var isPreviewRadialOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080C14)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header
        item {
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                Text(
                    text = "HIRIS Controller",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Cybernetic Assistant Dashboard",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF7E90A6)
                )
            }
        }

        // 2. Master Overlay Controller Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1525)),
                border = BorderStroke(1.dp, if (isOverlayRunning) CyanNeonPrimary.copy(alpha = 0.6f) else Color(0xFF1B2B45)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_master_overlay_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Floating Companion",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isOverlayRunning) "Active on your screen" else "Overlay currently stopped",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverlayRunning) CyanNeonPrimary else TextSecondary
                            )
                        }

                        // Status Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isOverlayRunning) Color(0xFF0D2938) else Color(0xFF161F2E),
                            border = BorderStroke(1.dp, if (isOverlayRunning) CyanNeonPrimary else Color(0xFF223148))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isOverlayRunning) Color(0xFF10B981) else Color(0xFF64748B))
                                )
                                Text(
                                    text = if (isOverlayRunning) "RUNNING" else "OFFLINE",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverlayRunning) Color(0xFF10B981) else Color(0xFF8B9FB3)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Master Start/Stop Button
                    Button(
                        onClick = {
                            if (isOverlayRunning) {
                                FloatingEyeOverlayService.stop(context)
                            } else {
                                if (!com.example.floatingeye.overlay.OverlayPermissionManager.canDrawOverlays(context)) {
                                    com.example.floatingeye.overlay.OverlayPermissionManager.openOverlayPermissionSettings(context)
                                } else {
                                    FloatingEyeOverlayService.start(context)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_toggle_floating_overlay"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverlayRunning) Color(0xFFDC2626) else CyanNeonPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isOverlayRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = if (isOverlayRunning) Color.White else CyanOnPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOverlayRunning) "STOP FLOATING OVERLAY" else "LAUNCH FLOATING HIRIS (+)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isOverlayRunning) Color.White else CyanOnPrimary
                        )
                    }
                }
            }
        }

        // 3. Quick Stats Metrics Grid (2x2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Metric 1: Active Extensions
                MetricCard(
                    title = "Extensions",
                    value = "$activeCount Active",
                    subtitle = "${extensions.size} available in market",
                    icon = Icons.Rounded.Extension,
                    iconTint = CyanNeonPrimary,
                    onClick = onNavigateToExtensions,
                    modifier = Modifier.weight(1f)
                )

                // Metric 2: Device Battery Telemetry
                MetricCard(
                    title = "Battery",
                    value = "${batteryInfo.percentage}%",
                    subtitle = if (batteryInfo.isCharging) "Charging active" else "Discharging",
                    icon = Icons.Rounded.BatteryChargingFull,
                    iconTint = if (batteryInfo.isCharging) CyanNeonPrimary else Color(0xFF38BDF8),
                    onClick = onNavigateToExtensions,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Interactive Widget Preview Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1524)),
                border = BorderStroke(1.dp, Color(0xFF1A263D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interactive Widget Preview",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap (+) to test",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanNeonPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Embedded interactive Plus Widget test
                    Box(
                        modifier = Modifier
                            .size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RadialAppShortcutMenu(
                            shortcuts = shortcuts,
                            isOpen = isPreviewRadialOpen,
                            isNearLeft = false,
                            isNearRight = false,
                            isMicPlaying = false,
                            micAudioLevel = 0f,
                            onToggleMic = {},
                            onLaunchShortcut = {}
                        )

                        FloatingEyePlusWidget(
                            size = overlaySize.sizeDp.dp,
                            isOpen = isPreviewRadialOpen,
                            isCharging = batteryInfo.isCharging && isBatteryActive,
                            isFull = batteryInfo.isFull && isBatteryActive,
                            batteryPercentage = batteryInfo.percentage,
                            neonColor = CyanNeonPrimary,
                            irisColor = Color(0xFF00B0FF),
                            emissivePulse = 1.0f,
                            glowIntensity = 1.0f,
                            modifier = Modifier
                                .clickable {
                                    if (isShortcutsActive) {
                                        isPreviewRadialOpen = !isPreviewRadialOpen
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Radial Shortcuts are inactive. Flip switch ON in Extensions Market.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isShortcutsActive) {
                            if (isPreviewRadialOpen) "Radial shortcuts menu opened" else "Click the button to test radial rotation"
                        } else {
                            "Shortcuts extension is inactive. Turn switch ON in Market to activate."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isShortcutsActive) TextSecondary else Color(0xFFF59E0B),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 5. Extensions Market Promo Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10192A)),
                border = BorderStroke(1.dp, Color(0xFF1E2D48)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToExtensions)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF15263E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Extension,
                            contentDescription = null,
                            tint = CyanNeonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Explore Extensions Market",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Discover gestures, appearance controls, power monitoring & more",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = "Go",
                        tint = CyanNeonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0E1524),
        border = BorderStroke(1.dp, Color(0xFF1B273F)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}
