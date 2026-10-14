package com.example.floatingeye.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.controller.FloatingEyeController
import com.example.floatingeye.controller.FloatingEyeLightController
import com.example.floatingeye.controller.FloatingEyeTouchController
import com.example.floatingeye.model.FloatingEyeConfig
import com.example.floatingeye.renderer.FloatingEyeRenderer
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.CyanPrimaryContainer
import com.example.ui.theme.StatusActiveCyan
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingEyeApp() {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Initialize configuration, controllers
    val config = remember { FloatingEyeConfig.loadFromAssets(context) }
    val lightController = remember { FloatingEyeLightController(config.bloomEnabled, config.bloomStrength) }
    val eyeController = remember { FloatingEyeController(config, lightController) }
    val touchController = remember { FloatingEyeTouchController(eyeController) }

    var showInfoSheet by remember { mutableStateOf(false) }

    val posX by eyeController.positionX.collectAsStateWithLifecycle()
    val posY by eyeController.positionY.collectAsStateWithLifecycle()
    val currentPitch by eyeController.currentPitch.collectAsStateWithLifecycle()
    val currentYaw by eyeController.currentYaw.collectAsStateWithLifecycle()
    val isTrackingTouch by eyeController.isTrackingTouch.collectAsStateWithLifecycle()
    val touchPoint by eyeController.touchPoint.collectAsStateWithLifecycle()
    val tapCount by eyeController.tapCount.collectAsStateWithLifecycle()
    val isPupilReacting by eyeController.isPupilReacting.collectAsStateWithLifecycle()
    val bloomEnabled by lightController.bloomEnabled.collectAsStateWithLifecycle()
    val bloomStrength by lightController.bloomStrength.collectAsStateWithLifecycle()
    val emissivePulse by lightController.emissivePulse.collectAsStateWithLifecycle()

    val eyeSizeDp = 220.dp
    val eyeSizePx = with(density) { eyeSizeDp.toPx() }

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Floating Eye",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            // Live status badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceContainerHigh,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isTrackingTouch) CyanNeonPrimary else StatusActiveCyan)
                                    )
                                    Text(
                                        text = if (isTrackingTouch) "TRACKING" else "IDLE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isTrackingTouch) CyanNeonPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                        Text(
                            text = "3D Autonomous Companion",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                },
                actions = {
                    // Center Eye button (48dp touch target, 24dp visual icon)
                    IconButton(
                        onClick = {
                            eyeController.centerEye(
                                screenWidth = eyeController.eyeWidthPx * 3.5f,
                                screenHeight = eyeController.eyeHeightPx * 3.5f
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("action_recenter")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CenterFocusStrong,
                            contentDescription = "Recenter Eye Position",
                            tint = CyanNeonPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Reset Gaze button
                    IconButton(
                        onClick = { eyeController.onTouchGazeReleased() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("action_reset_gaze")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = "Center Gaze",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Info button
                    IconButton(
                        onClick = { showInfoSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("action_info")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Model Diagnostics",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }

            // Initialize position and dimensions
            LaunchedEffect(screenWidthPx, screenHeightPx) {
                eyeController.updateScreenDimensions(eyeSizePx, eyeSizePx)
                val initialX = (screenWidthPx - eyeSizePx) / 2f
                val initialY = screenHeightPx * 0.08f
                eyeController.initializePosition(initialX, initialY)
            }

            // Layer 1: Background touch tracking workspace & visual canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(touchController.createGazeWorkspaceModifier())
            ) {
                // Subtle gaze tracking visual feedback
                GazeFeedbackCanvas(
                    eyeCenter = Offset(posX + eyeSizePx / 2f, posY + eyeSizePx / 2f),
                    touchPoint = touchPoint,
                    isTracking = isTrackingTouch
                )

                // Bottom Content / Diagnostics Deck
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    TelemetryDashboardCard(
                        pitch = currentPitch,
                        yaw = currentYaw,
                        isTracking = isTrackingTouch,
                        tapCount = tapCount,
                        isPupilReacting = isPupilReacting,
                        bloomEnabled = bloomEnabled,
                        bloomStrength = bloomStrength,
                        emissivePulse = emissivePulse,
                        onBloomToggle = { lightController.setBloomEnabled(it) },
                        onBloomStrengthChange = { lightController.setBloomStrength(it) },
                        onPulseTrigger = {
                            eyeController.onEyeTapped()
                        },
                        trackingParts = config.trackingParts
                    )
                }
            }

            // Layer 2: The Floating Eye (draggable, tap-detecting 3D element)
            Box(
                modifier = Modifier
                    .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
                    .size(eyeSizeDp)
                    .then(
                        touchController.createFloatingEyeTouchModifier(
                            screenWidthPx = screenWidthPx,
                            screenHeightPx = screenHeightPx,
                            eyeSizePx = eyeSizePx
                        )
                    )
                    .testTag("floating_eye_container"),
                contentAlignment = Alignment.Center
            ) {
                // Ambient cyan aura when active or tapped
                val auraAlpha = (0.12f * emissivePulse).coerceIn(0.08f, 0.45f)
                Box(
                    modifier = Modifier
                        .size(eyeSizeDp * 0.9f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    CyanNeonPrimary.copy(alpha = auraAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 3D Filament / SceneView Renderer
                FloatingEyeRenderer(
                    controller = eyeController,
                    lightController = lightController,
                    modifier = Modifier.fillMaxSize()
                )

                // Touch indicator overlay for subtle feedback
                if (isPupilReacting) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, CyanNeonPrimary.copy(alpha = 0.6f), CircleShape)
                    )
                }
            }
        }
    }

    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SurfaceContainerHigh,
            contentColor = TextPrimary
        ) {
            ModelInfoSheetContent(config = config) {
                showInfoSheet = false
            }
        }
    }
}

/**
 * Visual canvas rendering subtle gaze vector line and touch reticle.
 */
@Composable
private fun GazeFeedbackCanvas(
    eyeCenter: Offset,
    touchPoint: Pair<Float, Float>?,
    isTracking: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (isTracking && touchPoint != null) {
            val target = Offset(touchPoint.first, touchPoint.second)

            // Dotted gaze tracking ray
            drawLine(
                color = CyanNeonPrimary.copy(alpha = 0.35f),
                start = eyeCenter,
                end = target,
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
            )

            // Touch target reticle
            drawCircle(
                color = CyanNeonPrimary.copy(alpha = 0.15f),
                radius = 28.dp.toPx() * pulseScale,
                center = target
            )
            drawCircle(
                color = CyanNeonPrimary.copy(alpha = 0.8f),
                radius = 6.dp.toPx(),
                center = target
            )
            drawCircle(
                color = CyanNeonPrimary,
                radius = 2.5.dp.toPx(),
                center = target
            )
        }
    }
}

/**
 * Clean commercial telemetry and control card.
 * Adheres strictly to 8dp spacing system, 16-20dp rounded containers,
 * dark neutral surfaces and cyan accents.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TelemetryDashboardCard(
    pitch: Float,
    yaw: Float,
    isTracking: Boolean,
    tapCount: Int,
    isPupilReacting: Boolean,
    bloomEnabled: Boolean,
    bloomStrength: Float,
    emissivePulse: Float,
    onBloomToggle: (Boolean) -> Unit,
    onBloomStrengthChange: (Float) -> Unit,
    onPulseTrigger: () -> Unit,
    trackingParts: List<String>
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceContainer.copy(alpha = 0.94f)
        ),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = Brush.verticalGradient(
                colors = listOf(
                    CyanNeonPrimary.copy(alpha = 0.25f),
                    Color(0xFF222B38).copy(alpha = 0.3f)
                )
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("telemetry_card")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ocular Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isTracking) "Tracking Active Screen Touch" else "Touch Screen to Direct Gaze",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isTracking) CyanNeonPrimary else TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceContainerHigh,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = if (isExpanded) "Compact" else "Expand Deck",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanNeonPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Metrics Row (Pitch & Yaw Angles, Tap status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "Pitch (X)",
                    value = String.format("%.1f°", pitch),
                    modifier = Modifier.weight(1f),
                    isHighlighted = isTracking
                )
                MetricPill(
                    label = "Yaw (Y)",
                    value = String.format("%.1f°", yaw),
                    modifier = Modifier.weight(1f),
                    isHighlighted = isTracking
                )
                MetricPill(
                    label = "Tap Impulses",
                    value = "$tapCount",
                    modifier = Modifier.weight(1f),
                    isHighlighted = isPupilReacting
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Tracking Parts Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Eye Tracking Subsystems",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            trackingParts.forEach { part ->
                                val isCyanRing = part.contains("Cyan")
                                val isPupil = part.contains("Pupil")
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCyanRing) CyanPrimaryContainer.copy(alpha = 0.5f) else SurfaceContainerLow,
                                    border = if (isCyanRing) CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.linearGradient(listOf(CyanNeonPrimary, CyanNeonPrimary))
                                    ) else null
                                ) {
                                    Text(
                                        text = part,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCyanRing) CyanNeonPrimary else if (isPupil && isPupilReacting) CyanNeonPrimary else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bloom & Optical Configuration
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Flare,
                                    contentDescription = null,
                                    tint = CyanNeonPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Cyan Bloom (FE_NeonCyan)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                            Switch(
                                checked = bloomEnabled,
                                onCheckedChange = onBloomToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyanNeonPrimary,
                                    checkedTrackColor = CyanPrimaryContainer,
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = SurfaceContainerHigh
                                )
                            )
                        }

                        if (bloomEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Strength",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.width(55.dp)
                                )
                                Slider(
                                    value = bloomStrength,
                                    onValueChange = onBloomStrengthChange,
                                    valueRange = 0.1f..1.0f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyanNeonPrimary,
                                        activeTrackColor = CyanNeonPrimary,
                                        inactiveTrackColor = SurfaceContainerHigh
                                    )
                                )
                                Text(
                                    text = String.format("%.2f", bloomStrength),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanNeonPrimary,
                                    modifier = Modifier.width(36.dp)
                                )
                            }
                        }
                    }

                    // Pulse Test Action Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPulseTrigger() }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WbSunny,
                                contentDescription = null,
                                tint = CyanNeonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Trigger Emissive Cyan Surge",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlighted) SurfaceContainerHighest else SurfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = if (isHighlighted) CyanNeonPrimary else TextPrimary
            )
        }
    }
}

@Composable
private fun ModelInfoSheetContent(
    config: FloatingEyeConfig,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Model Specifications",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecRow("GLB Asset", config.assetPath)
                SpecRow("Configuration", "floating_eye.json")
                SpecRow("Luminous Material", config.emissiveMaterial)
                SpecRow("Max Pitch Deflection", "${config.maxDeflectionAngleXDeg}°")
                SpecRow("Max Yaw Deflection", "${config.maxDeflectionAngleYDeg}°")
                SpecRow("Tracking Nodes", "${config.trackingParts.size} parts")
            }
        }

        Text(
            text = "Tracking Nodes Isolation:",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                config.trackingParts.forEach { part ->
                    Text(
                        text = "• $part",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (part.contains("Cyan")) CyanNeonPrimary else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = CyanNeonPrimary)
    }
}
