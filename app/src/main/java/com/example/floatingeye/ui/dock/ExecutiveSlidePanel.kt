package com.example.floatingeye.ui.dock

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.floatingeye.extensions.model.ActiveExtensionItem
import com.example.floatingeye.extensions.repository.ExtensionRepository
import com.example.floatingeye.firewall.model.ActiveNetworkType
import com.example.floatingeye.shortcuts.AppShortcut
import com.example.floatingeye.shortcuts.InstalledAppInfo
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Slide-Out Executive Action Panel.
 * Provides instant access to Extensions (Firewall, Tasks, Sentinel, Mic)
 * and Pinned App Shortcuts in a refined, professional, dark-glass sidebar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExecutiveSlidePanel(
    isOpen: Boolean,
    isNearLeft: Boolean,
    onClose: () -> Unit,
    activeExtensions: List<ActiveExtensionItem>,
    onExtensionClick: (String) -> Unit,
    shortcuts: List<AppShortcut>,
    onLaunchShortcut: (AppShortcut) -> Unit,
    batteryPercentage: Int,
    isCharging: Boolean,
    networkType: ActiveNetworkType,
    isFirewallActive: Boolean,
    blockedAppsCount: Int,
    onToggleFirewall: (Boolean) -> Unit,
    isMicStreaming: Boolean,
    onToggleMic: () -> Unit,
    pendingTasksCount: Int,
    notificationCount: Int,
    drainMa: Int,
    onOpenFullApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClose()
            }
    ) {
        // Drawer Surface positioned on Left or Right depending on dock position
        Surface(
            shape = if (isNearLeft) {
                RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            } else {
                RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
            },
            color = Color(0xFF0C121D),
            border = BorderStroke(1.2.dp, Color(0xFF1E2D42)),
            shadowElevation = 24.dp,
            modifier = Modifier
                .align(if (isNearLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight(0.92f)
                .fillMaxWidth(0.85f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Prevent closing when clicking inside
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. Top Header Bar: Title, Status Pills, Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CyanNeonPrimary)
                        )
                        Text(
                            text = "Executive Panel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onOpenFullApp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = "Open Full App",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Panel",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Telemetry HUD Chips: Network, Battery, Shield
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Network Chip
                    StatusChip(
                        icon = if (networkType == ActiveNetworkType.WIFI) Icons.Rounded.Wifi else Icons.Rounded.NetworkCell,
                        text = when (networkType) {
                            ActiveNetworkType.WIFI -> "Wi-Fi"
                            ActiveNetworkType.MOBILE -> "Cellular"
                            ActiveNetworkType.NONE -> "Offline"
                        },
                        accentColor = CyanNeonPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    // Battery Chip
                    StatusChip(
                        icon = if (isCharging) Icons.Rounded.Bolt else Icons.Rounded.BatteryChargingFull,
                        text = "$batteryPercentage%${if (isCharging) " ⚡" else ""}",
                        accentColor = if (isCharging) Color(0xFFF59E0B) else Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )

                    // Shield Chip
                    StatusChip(
                        icon = Icons.Rounded.Shield,
                        text = if (isFirewallActive) "$blockedAppsCount Cut" else "Shield Off",
                        accentColor = if (isFirewallActive) Color(0xFF10B981) else TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Scrollable Tools & Extensions Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "EXTENSIONS & TOOLS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )

                    // Tool 1: Firewall & App Blocker Tile
                    ExtensionActionTile(
                        icon = Icons.Rounded.Shield,
                        title = "App Firewall",
                        subtitle = if (isFirewallActive) "$blockedAppsCount app(s) isolated from internet" else "Firewall disabled",
                        badgeText = if (isFirewallActive) "SHIELD ON" else null,
                        badgeColor = Color(0xFF10B981),
                        onClick = { onExtensionClick(ExtensionRepository.ID_FIREWALL) },
                        trailingContent = {
                            Switch(
                                checked = isFirewallActive,
                                onCheckedChange = onToggleFirewall,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF334155)
                                )
                            )
                        }
                    )

                    // Tool 3: Tasks & Smart Alarms Tile
                    ExtensionActionTile(
                        icon = Icons.Rounded.EventNote,
                        title = "Tasks & Reminders",
                        subtitle = if (pendingTasksCount > 0) "$pendingTasksCount pending task(s) with alarm" else "All tasks complete",
                        badgeText = if (pendingTasksCount > 0) "$pendingTasksCount DUE" else null,
                        badgeColor = Color(0xFFF59E0B),
                        onClick = { onExtensionClick(ExtensionRepository.ID_TASKS) }
                    )

                    // Tool 4: Live Bluetooth Mic Tile
                    ExtensionActionTile(
                        icon = Icons.Rounded.Mic,
                        title = "Live Mic Loopback",
                        subtitle = if (isMicStreaming) "Broadcasting live to audio output" else "Audio monitor ready",
                        badgeText = if (isMicStreaming) "LIVE" else null,
                        badgeColor = Color(0xFF06B6D4),
                        onClick = onToggleMic
                    )

                    // Tool 5: Notification Sentinel Tile
                    ExtensionActionTile(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Notification Sentinel",
                        subtitle = "$notificationCount system notifications preserved",
                        badgeText = if (notificationCount > 0) "$notificationCount LOGS" else null,
                        badgeColor = Color(0xFF8B5CF6),
                        onClick = { onExtensionClick(ExtensionRepository.ID_NOTIFICATIONS) }
                    )

                    // Tool 6: Power Drain Telemetry Tile
                    ExtensionActionTile(
                        icon = Icons.Rounded.Speed,
                        title = "Power Drain Monitor",
                        subtitle = "Instant current draw: ${drainMa}mA",
                        badgeText = "${drainMa}mA",
                        badgeColor = Color(0xFF0EA5E9),
                        onClick = { onExtensionClick(ExtensionRepository.ID_DRAIN_MONITOR) }
                    )

                    // 4. Pinned App Launcher Section
                    if (shortcuts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "PINNED SHORTCUTS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            shortcuts.forEach { shortcut ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF141F30),
                                    border = BorderStroke(1.dp, Color(0xFF223550)),
                                    modifier = Modifier.clickable {
                                        onClose()
                                        onLaunchShortcut(shortcut)
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(CyanNeonPrimary)
                                        )
                                        Text(
                                            text = shortcut.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Primary Action
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFullApp() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = CyanNeonPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open Hiris Controller Hub",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    icon: ImageVector,
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF141D2C),
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExtensionActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = CyanNeonPrimary,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF111A29),
        border = BorderStroke(1.dp, Color(0xFF1E2D42)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF18263A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (badgeText != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.15f),
                                border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingContent != null) {
                Box(modifier = Modifier.padding(start = 6.dp)) {
                    trailingContent()
                }
            }
        }
    }
}
