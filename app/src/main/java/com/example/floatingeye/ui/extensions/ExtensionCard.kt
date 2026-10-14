package com.example.floatingeye.ui.extensions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatingeye.extensions.model.ExtensionStatus
import com.example.floatingeye.extensions.model.HirisExtension
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtensionCard(
    extension: HirisExtension,
    onToggleActive: (Boolean) -> Unit,
    onInstall: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isInstalling by remember { mutableStateOf(false) }

    val cardBorderColor by animateColorAsState(
        targetValue = if (extension.isInstalled && extension.isActive) {
            Color(0xFF1E3252)
        } else {
            Color(0xFF182236)
        },
        animationSpec = tween(250),
        label = "cardBorderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("extension_card_${extension.id}")
            .clickable {
                if (extension.isInstalled) {
                    onOpenSettings()
                } else if (!isInstalling) {
                    isInstalling = true
                    onInstall()
                }
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E1524)
        ),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Extension Icon Box
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (extension.isInstalled && extension.isActive) {
                            Color(0xFF14243B)
                        } else {
                            Color(0xFF111B2C)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resolveExtensionIcon(extension.iconKey),
                    contentDescription = extension.name,
                    tint = if (extension.isInstalled && extension.isActive) {
                        CyanNeonPrimary
                    } else if (!extension.isInstalled) {
                        CyanNeonPrimary.copy(alpha = 0.85f)
                    } else {
                        Color(0xFF7E92AB)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 2. Info Column (Title, Description, Category Tags)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = extension.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = extension.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tags Row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    extension.categoryTags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF152033),
                            border = BorderStroke(0.8.dp, Color(0xFF1F2E49))
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color(0xFF7E93AC),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // 3. Actions & Status Column
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Status Badge Pill
                StatusBadge(status = extension.status)

                Spacer(modifier = Modifier.height(6.dp))

                // Action: Toggle switch + Gear OR Install Button
                if (extension.isInstalled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Switch(
                            checked = extension.isActive,
                            onCheckedChange = { active ->
                                onToggleActive(active)
                            },
                            modifier = Modifier.testTag("toggle_${extension.id}"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyanNeonPrimary,
                                checkedBorderColor = CyanNeonPrimary,
                                uncheckedThumbColor = Color(0xFF8899AC),
                                uncheckedTrackColor = Color(0xFF182335),
                                uncheckedBorderColor = Color(0xFF28364C)
                            )
                        )

                        if (extension.isActive) {
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF142034))
                                    .testTag("settings_${extension.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Configure ${extension.name}",
                                    tint = CyanNeonPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "Open ${extension.name}",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Install Button
                    Surface(
                        onClick = {
                            if (!isInstalling) {
                                isInstalling = true
                                onInstall()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F263C),
                        border = BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.85f)),
                        modifier = Modifier.testTag("install_${extension.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isInstalling) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = CyanNeonPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Adding...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = CyanNeonPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = "Install",
                                    tint = CyanNeonPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Install",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = CyanNeonPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ExtensionStatus) {
    val (dotColor, labelColor, labelText) = when (status) {
        ExtensionStatus.ACTIVE -> Triple(Color(0xFF10B981), Color(0xFF10B981), "Active")
        ExtensionStatus.OFF -> Triple(Color(0xFF64748B), Color(0xFF64748B), "Off")
        ExtensionStatus.NEW -> Triple(CyanNeonPrimary, CyanNeonPrimary, "New")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.5.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = labelColor
        )
    }
}

private fun resolveExtensionIcon(iconKey: String): ImageVector {
    return when (iconKey) {
        "shortcuts" -> Icons.Rounded.Widgets
        "gestures" -> Icons.Rounded.Gesture
        "battery" -> Icons.Rounded.BatteryChargingFull
        "drain" -> Icons.Rounded.Speed
        "appearance" -> Icons.Rounded.Palette
        "mic" -> Icons.Rounded.Mic
        "notifications" -> Icons.Rounded.NotificationsActive
        "tasks" -> Icons.Rounded.EventNote
        "download" -> Icons.Rounded.Download
        "firewall" -> Icons.Rounded.Shield
        else -> Icons.Rounded.Widgets
    }
}
