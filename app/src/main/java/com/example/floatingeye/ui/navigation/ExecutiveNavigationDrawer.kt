package com.example.floatingeye.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ExecutiveNavigationDrawerContent(
    currentScreen: HirisNavScreen,
    isOverlayRunning: Boolean,
    isFirewallActive: Boolean,
    blockedAppsCount: Int,
    batteryPercentage: Int,
    activeModulesCount: Int,
    onNavigate: (HirisNavScreen) -> Unit,
    onToggleOverlay: () -> Unit,
    onToggleFirewall: () -> Unit,
    onOpenModuleConfig: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight(),
        drawerContainerColor = Color(0xFF070B13),
        drawerContentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // 1. Header with branding & close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F1A2E), Color(0xFF070B13))
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0A2239))
                                    .border(BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.5f)), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = CyanNeonPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "HIRIS PRO OS",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "Executive System Engine",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8195B0)
                                )
                            }
                        }

                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier.testTag("drawer_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Menu",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // System Telemetry Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Battery badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF131F33),
                            border = BorderStroke(1.dp, Color(0xFF1E2E48)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = CyanNeonPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "$batteryPercentage%",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Shield status badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFirewallActive) Color(0xFF0C2A1E) else Color(0xFF1F1D1A),
                            border = BorderStroke(
                                1.dp,
                                if (isFirewallActive) Color(0xFF10B981) else Color(0xFF78350F)
                            ),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isFirewallActive) Color(0xFF10B981) else Color(0xFFF59E0B))
                                )
                                Text(
                                    text = if (isFirewallActive) "SHIELD ($blockedAppsCount)" else "SHIELD OFF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFirewallActive) Color(0xFF34D399) else Color(0xFFFBBF24)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = Color(0xFF141F30))

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Master System Switches
            Text(
                text = "SYSTEM CONTROLS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7F96),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Master Overlay Switch Item
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0E1624),
                border = BorderStroke(1.dp, Color(0xFF18263D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = if (isOverlayRunning) CyanNeonPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Floating Eye HUD",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = if (isOverlayRunning) "Running on display" else "Stopped",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isOverlayRunning,
                        onCheckedChange = { onToggleOverlay() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanNeonPrimary,
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("drawer_overlay_switch")
                    )
                }
            }

            // Master Firewall Shield Switch Item
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0E1624),
                border = BorderStroke(1.dp, Color(0xFF18263D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = if (isFirewallActive) Color(0xFF10B981) else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Network Data Shield",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = if (isFirewallActive) "Drop rules active" else "Unrestricted",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isFirewallActive,
                        onCheckedChange = { onToggleFirewall() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("drawer_firewall_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF141F30))
            Spacer(modifier = Modifier.height(8.dp))

            // 3. Main Navigation Items
            Text(
                text = "NAVIGATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7F96),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            DrawerNavigationItem(
                label = "Dashboard Overview",
                icon = Icons.Rounded.Dashboard,
                isSelected = currentScreen == HirisNavScreen.HOME,
                onClick = {
                    onNavigate(HirisNavScreen.HOME)
                    onCloseDrawer()
                }
            )

            DrawerNavigationItem(
                label = "Module Manager",
                icon = Icons.Rounded.Widgets,
                isSelected = currentScreen == HirisNavScreen.EXTENSIONS,
                badge = "$activeModulesCount Active",
                onClick = {
                    onNavigate(HirisNavScreen.EXTENSIONS)
                    onCloseDrawer()
                }
            )

            DrawerNavigationItem(
                label = "HUD Optics & Calibration",
                icon = Icons.Rounded.Visibility,
                isSelected = currentScreen == HirisNavScreen.EYE,
                onClick = {
                    onNavigate(HirisNavScreen.EYE)
                    onCloseDrawer()
                }
            )

            DrawerNavigationItem(
                label = "System Diagnostics & Specs",
                icon = Icons.Rounded.Settings,
                isSelected = currentScreen == HirisNavScreen.PROFILE,
                onClick = {
                    onNavigate(HirisNavScreen.PROFILE)
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF141F30))
            Spacer(modifier = Modifier.height(8.dp))

            // 4. Quick Module Panels (Direct Launch)
            Text(
                text = "DIRECT MODULE PANELS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7F96),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            DrawerModuleShortcutItem(
                title = "Firewall & App Blocker",
                subtitle = "Block Mobile & Wi-Fi traffic",
                icon = Icons.Rounded.Security,
                iconTint = Color(0xFF10B981),
                onClick = {
                    onOpenModuleConfig("firewall")
                    onCloseDrawer()
                }
            )

            DrawerModuleShortcutItem(
                title = "Media Downloader",
                subtitle = "MP4 & MP3 direct fetch",
                icon = Icons.Rounded.FileDownload,
                iconTint = CyanNeonPrimary,
                onClick = {
                    onOpenModuleConfig("media_downloader")
                    onCloseDrawer()
                }
            )

            DrawerModuleShortcutItem(
                title = "Task Sentinel & Alarms",
                subtitle = "Scheduled auditory alarms",
                icon = Icons.Rounded.Alarm,
                iconTint = Color(0xFFF59E0B),
                onClick = {
                    onOpenModuleConfig("tasks_reminders")
                    onCloseDrawer()
                }
            )

            DrawerModuleShortcutItem(
                title = "Power Drain Telemetry",
                subtitle = "Current mA & consumption",
                icon = Icons.Rounded.ElectricBolt,
                iconTint = Color(0xFFEAB308),
                onClick = {
                    onOpenModuleConfig("power_drain_monitor")
                    onCloseDrawer()
                }
            )

            DrawerModuleShortcutItem(
                title = "Notification Sentinel",
                subtitle = "Capture & vault incoming chats",
                icon = Icons.Rounded.NotificationsActive,
                iconTint = Color(0xFFA855F7),
                onClick = {
                    onOpenModuleConfig("notification_sentinel")
                    onCloseDrawer()
                }
            )

            DrawerModuleShortcutItem(
                title = "Radial Quick Launch",
                subtitle = "Fast app launcher menu",
                icon = Icons.Rounded.Speed,
                iconTint = Color(0xFF38BDF8),
                onClick = {
                    onOpenModuleConfig("shortcuts")
                    onCloseDrawer()
                }
            )

            DrawerModuleShortcutItem(
                title = "Bluetooth Audio Streamer",
                subtitle = "Microphone monitor stream",
                icon = Icons.Rounded.Mic,
                iconTint = Color(0xFFEC4899),
                onClick = {
                    onOpenModuleConfig("bluetooth_mic")
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Footer branding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "HIRIS Architecture v2.4 • Native Kotlin",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
                Text(
                    text = "Material 3 Enterprise Design System",
                    fontSize = 10.sp,
                    color = Color(0xFF334155)
                )
            }
        }
    }
}

@Composable
private fun DrawerNavigationItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF101D33) else Color.Transparent,
        animationSpec = tween(180),
        label = "drawerItemBg"
    )
    val contentColor = if (isSelected) CyanNeonPrimary else Color(0xFFCBD5E1)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = if (isSelected) BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.4f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    fontSize = 13.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            }

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeonPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerModuleShortcutItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF090E17),
        border = BorderStroke(1.dp, Color(0xFF141F30)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE2E8F0)
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
