package com.example.floatingeye.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatingeye.overlay.OverlayPermissionManager
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Cyberpunk Permission Prompt Screen for the Overlay (SYSTEM_ALERT_WINDOW) Capability.
 */
@Composable
fun OverlayPermissionScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasPermission = OverlayPermissionManager.canDrawOverlays(context)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("overlay_permission_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1525)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Cyber Icon Circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    CyanNeonPrimary.copy(alpha = 0.25f),
                                    Color(0xFF0E1A2D)
                                )
                            )
                        )
                        .border(1.dp, CyanNeonPrimary.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = "Overlay Permission",
                        tint = CyanNeonPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Display Over Other Apps",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "HIRIS Controller operates as a floating cybernetic companion. Grant overlay permission so the (+) button and gesture canvas can float over any open screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(26.dp))

                Button(
                    onClick = {
                        if (!hasPermission) {
                            OverlayPermissionManager.openOverlayPermissionSettings(context)
                        } else {
                            onContinue()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_grant_overlay_permission"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanNeonPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = CyanOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (hasPermission) "PERMISSION GRANTED — CONTINUE" else "ENABLE OVERLAY ACCESS",
                        fontWeight = FontWeight.Bold,
                        color = CyanOnPrimary,
                        fontSize = 13.5.sp
                    )
                }

                if (!hasPermission) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        onClick = onContinue,
                        color = Color.Transparent
                    ) {
                        Text(
                            text = "Continue to Dashboard (Preview Mode)",
                            color = TextTertiary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
