package com.example.floatingeye.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.NetworkWifi
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SignalCellularOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatingeye.firewall.model.ActiveNetworkType
import com.example.floatingeye.firewall.model.FirewallAppRule
import com.example.floatingeye.firewall.repository.FirewallRepository
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

data class QuickAppPreset(
    val packageName: String,
    val displayName: String,
    val iconColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlidingQuickAccessSheet(
    firewallRepository: FirewallRepository,
    isOverlayRunning: Boolean,
    isFirewallActive: Boolean,
    onToggleOverlay: () -> Unit,
    onToggleFirewall: () -> Unit,
    onOpenFullFirewall: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val activeNetwork by firewallRepository.activeNetworkType.collectAsStateWithLifecycle()
    val rules by firewallRepository.rules.collectAsStateWithLifecycle()

    val commonPresets = listOf(
        QuickAppPreset("com.whatsapp", "WhatsApp", Color(0xFF25D366)),
        QuickAppPreset("com.facebook.katana", "Facebook", Color(0xFF1877F2)),
        QuickAppPreset("com.instagram.android", "Instagram", Color(0xFFE4405F)),
        QuickAppPreset("com.google.android.youtube", "YouTube", Color(0xFFFF0000)),
        QuickAppPreset("com.zhiliaoapp.musically", "TikTok", Color(0xFF00F2FE)),
        QuickAppPreset("com.android.chrome", "Chrome", Color(0xFFFBBF24))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF080D17),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2D45))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D253A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = CyanNeonPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Quick Control Panel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Instant Network Shields & Floating HUD",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Current Network Indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF131D2D),
                    border = BorderStroke(1.dp, Color(0xFF223654))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (activeNetwork) {
                                ActiveNetworkType.WIFI -> Icons.Rounded.NetworkWifi
                                ActiveNetworkType.MOBILE -> Icons.Rounded.NetworkCell
                                else -> Icons.Rounded.Block
                            },
                            contentDescription = null,
                            tint = if (activeNetwork != ActiveNetworkType.NONE) CyanNeonPrimary else Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = activeNetwork.name,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF141F30))
            Spacer(modifier = Modifier.height(14.dp))

            // Master Switches Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Floating Eye Switch Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F1726),
                    border = BorderStroke(1.dp, if (isOverlayRunning) CyanNeonPrimary.copy(alpha = 0.5f) else Color(0xFF1E2D45)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = if (isOverlayRunning) CyanNeonPrimary else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Switch(
                                checked = isOverlayRunning,
                                onCheckedChange = { onToggleOverlay() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = CyanNeonPrimary
                                ),
                                modifier = Modifier.testTag("quick_sheet_overlay_switch")
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Floating Eye",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isOverlayRunning) "Running" else "Stopped",
                            fontSize = 10.5.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Firewall Master Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F1726),
                    border = BorderStroke(1.dp, if (isFirewallActive) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF1E2D45)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = if (isFirewallActive) Color(0xFF10B981) else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Switch(
                                checked = isFirewallActive,
                                onCheckedChange = { onToggleFirewall() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color(0xFF10B981)
                                ),
                                modifier = Modifier.testTag("quick_sheet_firewall_switch")
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Firewall Shield",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isFirewallActive) "Drop Rules ON" else "Disabled",
                            fontSize = 10.5.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick App Blocking Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ONE-TAP APP DATA SHIELD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7F96),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Tap to cut off network",
                    fontSize = 10.5.sp,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal row of quick toggles for WhatsApp, Facebook, Instagram, YouTube, etc.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(commonPresets) { preset ->
                    val rule = rules[preset.packageName]
                    val isBlocked = rule?.isAnyBlocked == true

                    val blockBg by animateColorAsState(
                        targetValue = if (isBlocked) Color(0xFF261214) else Color(0xFF0F1726),
                        animationSpec = tween(180),
                        label = "quickAppBg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isBlocked) Color(0xFFEF4444) else Color(0xFF1E2D45),
                        animationSpec = tween(180),
                        label = "quickAppBorder"
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = blockBg,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .width(108.dp)
                            .clickable {
                                val currentRule = rules[preset.packageName]
                                val newBlocked = !(currentRule?.isAnyBlocked == true)
                                firewallRepository.setAppRule(
                                    packageName = preset.packageName,
                                    appName = preset.displayName,
                                    blockMobile = newBlocked,
                                    blockWifi = newBlocked
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(preset.iconColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.displayName.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = preset.iconColor,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = preset.displayName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBlocked) Icons.Rounded.SignalCellularOff else Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isBlocked) Color(0xFFEF4444) else Color(0xFF10B981),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (isBlocked) "BLOCKED" else "ONLINE",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBlocked) Color(0xFFEF4444) else Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Open Full Firewall Settings Button
            Button(
                onClick = {
                    onDismiss()
                    onOpenFullFirewall()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF132034),
                    contentColor = CyanNeonPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF223654))
            ) {
                Text(
                    text = "Open Advanced App Shield Rules",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
