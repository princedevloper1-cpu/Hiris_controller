package com.example.floatingeye.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.R

@Composable
fun ExecutiveTopAppBar(
    currentScreen: HirisNavScreen,
    isOverlayRunning: Boolean,
    isFirewallActive: Boolean,
    blockedAppsCount: Int,
    onMenuClick: () -> Unit,
    onToggleOverlay: () -> Unit,
    onOpenQuickPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF070B13))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Professional Menu Button & Screen Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Menu Button with subtle border and touch target
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F1726),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D45)),
                    modifier = Modifier.size(42.dp),
                    onClick = onMenuClick
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.testTag("top_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = CyanNeonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Title & Context Subtitle
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "HIRIS PRO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Live status pill
                        val shieldColor by animateColorAsState(
                            targetValue = if (isFirewallActive) Color(0xFF10B981) else Color(0xFF475569),
                            animationSpec = tween(200),
                            label = "shieldPillColor"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(shieldColor.copy(alpha = 0.15f))
                                .border(1.dp, shieldColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(if (isFirewallActive) R.string.shield_on else R.string.shield_off),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = shieldColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Text(
                        text = stringResource(when (currentScreen) {
                            HirisNavScreen.HOME -> R.string.dashboard_subtitle
                            HirisNavScreen.EXTENSIONS -> R.string.modules_subtitle
                            HirisNavScreen.EYE -> R.string.hud_subtitle
                            HirisNavScreen.PROFILE -> R.string.diagnostics_subtitle
                        }),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = TextSecondary
                    )
                }
            }

            // Right Actions: Quick Overlay Toggle & Quick Access Drawer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Floating Eye Toggle Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOverlayRunning) Color(0xFF08263B) else Color(0xFF0F1726),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isOverlayRunning) CyanNeonPrimary.copy(alpha = 0.6f) else Color(0xFF1E2D45)
                    ),
                    modifier = Modifier.size(42.dp),
                    onClick = onToggleOverlay
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.testTag("top_overlay_toggle")
                    ) {
                        Icon(
                            imageVector = if (isOverlayRunning) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = "Toggle Floating Eye",
                            tint = if (isOverlayRunning) CyanNeonPrimary else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Quick Sliding Access Panel Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F1726),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D45)),
                    modifier = Modifier.size(42.dp),
                    onClick = onOpenQuickPanel
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.testTag("top_quick_panel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = "Slide Quick Controls",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Thin division line
        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFF141F30)
        )
    }
}
