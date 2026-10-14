package com.example.floatingeye.shortcuts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatingeye.extensions.model.ActiveExtensionItem
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders user app shortcuts and all activated extensions in an orbiting radial layout around the Floating Eye.
 * When the user clicks the floating icon, all active extensions pop up like dedicated app icons.
 */
@Composable
fun RadialAppShortcutMenu(
    shortcuts: List<AppShortcut>,
    isOpen: Boolean,
    isNearLeft: Boolean,
    isNearRight: Boolean,
    activeExtensions: List<ActiveExtensionItem> = emptyList(),
    onActiveExtensionClick: ((ActiveExtensionItem) -> Unit)? = null,
    isMicPlaying: Boolean = false,
    micAudioLevel: Float = 0f,
    onToggleMic: (() -> Unit)? = null,
    notificationCount: Int = 0,
    onOpenNotifications: (() -> Unit)? = null,
    onLaunchShortcut: (AppShortcut) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedAppsRepo = remember { InstalledAppsRepository(context) }

    // Keep at most eight app and extension icons around the central eye.
    // Interleave both kinds so they form one balanced orbit, like app icons
    // around a launcher rather than two separate clusters.
    val maxOrbitItems = 8
    val displayShortcuts = shortcuts.take(maxOrbitItems)
    val displayExtensions = activeExtensions.take((maxOrbitItems - displayShortcuts.size).coerceAtLeast(0))
    val shortcutCount = displayShortcuts.size
    val extensionCount = displayExtensions.size
    val totalIconCount = shortcutCount + extensionCount
    val orbitStartAngle = when {
        isNearLeft -> -90.0
        isNearRight -> 90.0
        else -> -90.0
    }
    val shortcutAngles = MutableList(shortcutCount) { 0.0 }
    val extAngles = MutableList(extensionCount) { 0.0 }
    var shortcutIndex = 0
    var extensionIndex = 0
    for (slot in 0 until totalIconCount) {
        val nextShortcutRatio = if (shortcutIndex < shortcutCount) {
            (shortcutIndex + 1).toFloat() / shortcutCount
        } else Float.POSITIVE_INFINITY
        val nextExtensionRatio = if (extensionIndex < extensionCount) {
            (extensionIndex + 1).toFloat() / extensionCount
        } else Float.POSITIVE_INFINITY
        val angle = orbitStartAngle + slot * (360.0 / totalIconCount)
        if (nextShortcutRatio <= nextExtensionRatio) {
            shortcutAngles[shortcutIndex++] = angle
        } else {
            extAngles[extensionIndex++] = angle
        }
    }

    val radialDistanceDp = 96.dp
    val extRadialDistanceDp = 96.dp

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // 1. App Shortcuts Orbiting Arc
        displayShortcuts.forEachIndexed { index, shortcut ->
            val angleDeg = shortcutAngles.getOrElse(index) { 90.0 }
            val angleRad = Math.toRadians(angleDeg)

            val animFactor by animateFloatAsState(
                targetValue = if (isOpen) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "radial_anim_$index"
            )

            val currentDistanceDp = radialDistanceDp * animFactor
            val offsetX = (currentDistanceDp.value * cos(angleRad)).dp
            val offsetY = (currentDistanceDp.value * sin(angleRad)).dp

            AnimatedVisibility(
                visible = isOpen,
                enter = scaleIn(initialScale = 0.2f) + fadeIn(),
                exit = scaleOut(targetScale = 0.2f) + fadeOut()
            ) {
                RadialShortcutItem(
                    shortcut = shortcut,
                    drawable = remember(shortcut.packageName) {
                        installedAppsRepo.getAppIcon(shortcut.packageName)
                    },
                    modifier = Modifier.offset(x = offsetX, y = offsetY),
                    onLaunch = { onLaunchShortcut(shortcut) }
                )
            }
        }

        // 2. Activated Extensions Orbiting Arc (Mounting dynamically like Apps!)
        displayExtensions.forEachIndexed { index, extItem ->
            val angleDeg = extAngles.getOrElse(index) { -90.0 }
            val angleRad = Math.toRadians(angleDeg)

            val animFactor by animateFloatAsState(
                targetValue = if (isOpen) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "radial_ext_anim_${extItem.id}"
            )

            val currentDistanceDp = extRadialDistanceDp * animFactor
            val offsetX = (currentDistanceDp.value * cos(angleRad)).dp
            val offsetY = (currentDistanceDp.value * sin(angleRad)).dp

            AnimatedVisibility(
                visible = isOpen,
                enter = scaleIn(initialScale = 0.2f) + fadeIn(),
                exit = scaleOut(targetScale = 0.2f) + fadeOut()
            ) {
                RadialExtensionItem(
                    extensionItem = extItem,
                    isMicPlaying = isMicPlaying && extItem.id == "ext_bluetooth_mic",
                    micAudioLevel = micAudioLevel,
                    onClick = {
                        onActiveExtensionClick?.invoke(extItem)
                    },
                    modifier = Modifier.offset(x = offsetX, y = offsetY)
                )
            }
        }

        // Fallback standalone Mic if not in activeExtensions list
        if (onToggleMic != null && activeExtensions.none { it.id == "ext_bluetooth_mic" }) {
            val micAngleRad = Math.toRadians(if (isNearLeft) -35.0 else if (isNearRight) 215.0 else -90.0)
            val micAnimFactor by animateFloatAsState(
                targetValue = if (isOpen) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "radial_mic_anim"
            )
            val micDistanceDp = 76.dp * micAnimFactor
            val micOffsetX = (micDistanceDp.value * cos(micAngleRad)).dp
            val micOffsetY = (micDistanceDp.value * sin(micAngleRad)).dp

            AnimatedVisibility(
                visible = isOpen,
                enter = scaleIn(initialScale = 0.2f) + fadeIn(),
                exit = scaleOut(targetScale = 0.2f) + fadeOut()
            ) {
                RadialMicButtonItem(
                    isPlaying = isMicPlaying,
                    audioLevel = micAudioLevel,
                    onClick = onToggleMic,
                    modifier = Modifier.offset(x = micOffsetX, y = micOffsetY)
                )
            }
        }

        // Fallback standalone Notification Sentinel button if not in activeExtensions list
        if (onOpenNotifications != null && activeExtensions.none { it.id == "ext_notifications" }) {
            val notifAngleRad = Math.toRadians(if (isNearLeft) 115.0 else if (isNearRight) 65.0 else -30.0)
            val notifAnimFactor by animateFloatAsState(
                targetValue = if (isOpen) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "radial_notif_anim"
            )
            val notifDistanceDp = 76.dp * notifAnimFactor
            val notifOffsetX = (notifDistanceDp.value * cos(notifAngleRad)).dp
            val notifOffsetY = (notifDistanceDp.value * sin(notifAngleRad)).dp

            AnimatedVisibility(
                visible = isOpen,
                enter = scaleIn(initialScale = 0.2f) + fadeIn(),
                exit = scaleOut(targetScale = 0.2f) + fadeOut()
            ) {
                RadialNotificationButtonItem(
                    count = notificationCount,
                    onClick = onOpenNotifications,
                    modifier = Modifier.offset(x = notifOffsetX, y = notifOffsetY)
                )
            }
        }
    }
}

/**
 * Dedicated app-like button for an activated extension mounted on the floating eye.
 */
@Composable
private fun RadialExtensionItem(
    extensionItem: ActiveExtensionItem,
    isMicPlaying: Boolean = false,
    micAudioLevel: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val auraScale by animateFloatAsState(
        targetValue = if (isMicPlaying) 1f + micAudioLevel * 0.45f else 1f,
        label = "ext_aura_scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // App name tooltip on press/tap
        AnimatedVisibility(
            visible = isPressed,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.offset(y = (-36).dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceDark.copy(alpha = 0.95f),
                border = SurfaceDefaultsBorder()
            ) {
                Text(
                    text = extensionItem.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanNeonPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
        }

        // Soundwave pulse for mic
        if (isMicPlaying) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .scale(auraScale)
                    .background(Color(0xFF38E07B).copy(alpha = 0.28f), CircleShape)
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isMicPlaying) Color(0xFF003D20) else SurfaceContainerHigh,
            border = androidx.compose.foundation.BorderStroke(
                1.8.dp,
                if (isMicPlaying) Color(0xFF38E07B) else CyanNeonPrimary
            ),
            shadowElevation = 8.dp,
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .testTag("radial_ext_${extensionItem.id}")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isMicPlaying) Icons.Rounded.GraphicEq else extensionItem.icon,
                        contentDescription = extensionItem.name,
                        tint = if (isMicPlaying) Color(0xFF38E07B) else CyanNeonPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isMicPlaying) "LIVE" else extensionItem.name.take(4).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (isMicPlaying) Color(0xFF38E07B) else CyanNeonPrimary,
                        maxLines = 1
                    )
                }

                // Notification badge or telemetry badge if any
                if (!extensionItem.badgeText.isNullOrEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                    ) {
                        Text(
                            text = extensionItem.badgeText,
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 0.5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadialNotificationButtonItem(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = SurfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(2.dp, CyanNeonPrimary),
        shadowElevation = 8.dp,
        onClick = onClick,
        modifier = modifier
            .size(54.dp)
            .testTag("btn_radial_notifications")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = "Notifications",
                tint = CyanNeonPrimary,
                modifier = Modifier.size(24.dp)
            )
            if (count > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                ) {
                    Text(
                        text = if (count > 9) "9+" else "$count",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RadialMicButtonItem(
    isPlaying: Boolean,
    audioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val auraScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f + audioLevel * 0.45f else 1f,
        label = "mic_aura_scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .scale(auraScale)
                    .background(Color(0xFF38E07B).copy(alpha = 0.28f), CircleShape)
            )
        }

        Surface(
            shape = CircleShape,
            color = if (isPlaying) Color(0xFF003D20) else SurfaceContainerHigh,
            border = androidx.compose.foundation.BorderStroke(
                2.5.dp,
                if (isPlaying) Color(0xFF38E07B) else CyanNeonPrimary
            ),
            shadowElevation = 10.dp,
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .testTag("btn_radial_mic")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(2.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                    contentDescription = "Live Bluetooth Mic",
                    tint = if (isPlaying) Color(0xFF38E07B) else CyanNeonPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isPlaying) "LIVE" else "MIC",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
                    color = if (isPlaying) Color(0xFF38E07B) else CyanNeonPrimary
                )
            }
        }
    }
}

@Composable
private fun RadialShortcutItem(
    shortcut: AppShortcut,
    drawable: android.graphics.drawable.Drawable?,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isPressed,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.offset(y = (-36).dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceDark.copy(alpha = 0.95f),
                border = SurfaceDefaultsBorder()
            ) {
                Text(
                    text = shortcut.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanNeonPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainerHigh,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                CyanNeonPrimary.copy(alpha = 0.5f)
            ),
            shadowElevation = 6.dp,
            onClick = onLaunch,
            modifier = Modifier
                .size(50.dp)
                .testTag("radial_shortcut_${shortcut.packageName}")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                DrawableAppIcon(
                    drawable = drawable,
                    contentDescription = shortcut.displayName,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            }
        }
    }
}

@Composable
private fun SurfaceDefaultsBorder() = androidx.compose.foundation.BorderStroke(
    1.dp,
    CyanNeonPrimary.copy(alpha = 0.5f)
)
