package com.example.floatingeye.ui.dock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Executive Floating Dock Handle.
 * Replaces cartoon/robot sci-fi eyes with a high-end, matte titanium assistive dock.
 * Features:
 * - CNC Titanium / Frosted Obsidian bezel.
 * - Material Symbols multi-action glyph.
 * - Subtle live status pips for active firewall or mic.
 * - Idle auto-dimming for distraction-free reading and gaming.
 */
@Composable
fun ExecutiveFloatingDock(
    size: Dp = 56.dp,
    isOpen: Boolean = false,
    isCharging: Boolean = false,
    batteryPercentage: Int = 100,
    isFirewallActive: Boolean = false,
    isMicActive: Boolean = false,
    isNotifActive: Boolean = false,
    activeExtensionCount: Int = 0,
    modifier: Modifier = Modifier
) {
    var isInteracting by remember { mutableStateOf(false) }

    // Auto-dim when idle for 3.5 seconds
    var isDimmed by remember { mutableStateOf(false) }
    LaunchedEffect(isInteracting, isOpen) {
        if (isOpen) {
            isDimmed = false
        } else {
            isDimmed = false
            delay(3500)
            isDimmed = true
        }
    }

    val dockAlpha by animateFloatAsState(
        targetValue = if (isDimmed && !isOpen) 0.42f else 1f,
        animationSpec = spring(stiffness = 300f),
        label = "dock_alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isOpen) 0.94f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "dock_scale"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0A0F17),
        border = BorderStroke(
            1.2.dp,
            if (isOpen) Color(0xFF00D2FF).copy(alpha = 0.8f) else Color(0xFF1E2B3E)
        ),
        shadowElevation = 10.dp,
        modifier = modifier
            .size(size)
            .alpha(dockAlpha)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF141F30),
                            Color(0xFF0A0F17)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Center Precision Menu Glyph
            Icon(
                imageVector = Icons.Rounded.DashboardCustomize,
                contentDescription = "Hiris Menu",
                tint = if (isOpen) Color(0xFF00D2FF) else Color(0xFFE2E8F0),
                modifier = Modifier.size((size.value * 0.44f).dp)
            )

            // Top-right Micro Telemetry Pill
            if (isFirewallActive || isMicActive || isCharging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isFirewallActive -> Color(0xFF10B981) // Green Shield
                                isMicActive -> Color(0xFF00D2FF)      // Cyan Mic
                                isCharging -> Color(0xFFF59E0B)       // Amber Charging
                                else -> Color(0xFF38BDF8)
                            }
                        )
                )
            }

            // Bottom Micro Badge for active tools
            if (activeExtensionCount > 0 && !isOpen) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(0.8.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                ) {
                    Text(
                        text = "$activeExtensionCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                    )
                }
            }
        }
    }
}
