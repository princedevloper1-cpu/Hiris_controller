package com.example.floatingeye.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatingeye.audio.BluetoothMicController
import com.example.floatingeye.notifications.NotificationPermissionHelper
import com.example.floatingeye.overlay.OverlayPermissionManager
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ProfileScreen(
    micController: BluetoothMicController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasOverlay = OverlayPermissionManager.canDrawOverlays(context)
    val hasNotif = NotificationPermissionHelper.isNotificationAccessGranted(context)
    val hasMic = micController.hasRecordPermission()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080C14)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                Text(
                    text = "Profile & System",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "System Diagnostics, Permissions & Device Info",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF7E90A6)
                )
            }
        }

        // Profile Avatar Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1525)),
                border = BorderStroke(1.dp, Color(0xFF1B2B45)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF14243C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = CyanNeonPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HIRIS User",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Cybernetic Controller • Build 2.4",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanNeonPrimary
                        )
                        Text(
                            text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        item {
            HirisAccountCard()
        }

        // System Permissions Diagnostics
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1524)),
                border = BorderStroke(1.dp, Color(0xFF1A263D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "System Permissions Hub",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    PermissionRow(
                        title = "Display Over Other Apps",
                        subtitle = "Required to render the floating (+) button",
                        isGranted = hasOverlay,
                        icon = Icons.Rounded.Visibility,
                        onFix = { OverlayPermissionManager.openOverlayPermissionSettings(context) }
                    )

                    PermissionRow(
                        title = "Notification Access",
                        subtitle = "Allows Notification Sentinel history capture",
                        isGranted = hasNotif,
                        icon = Icons.Rounded.Notifications,
                        onFix = { NotificationPermissionHelper.openNotificationListenerSettings(context) }
                    )

                    PermissionRow(
                        title = "Microphone Record",
                        subtitle = "Required for Bluetooth audio loopback streaming",
                        isGranted = hasMic,
                        icon = Icons.Rounded.Mic,
                        onFix = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }

        // About HIRIS Architecture
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1524)),
                border = BorderStroke(1.dp, Color(0xFF1A263D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About HIRIS Controller",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "HIRIS Controller is an Android overlay companion with built-in modules for shortcuts, gestures, battery visualization, reminders, media downloads, notifications, and network controls.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Framework", color = TextTertiary, fontSize = 12.sp)
                        Text("Jetpack Compose M3", color = CyanNeonPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version", color = TextTertiary, fontSize = 12.sp)
                        Text("2.4 (Pro Edition)", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onFix: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF11192A),
        border = BorderStroke(1.dp, if (isGranted) Color(0xFF1C2C46) else Color(0xFF332026)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) CyanNeonPrimary else Color(0xFFF87171),
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary, fontSize = 11.sp)
            }

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F2622)
                ) {
                    Text(
                        text = "Granted",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onFix,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFF87171))
                ) {
                    Text("Grant", color = Color(0xFFF87171), fontSize = 11.sp)
                }
            }
        }
    }
}
