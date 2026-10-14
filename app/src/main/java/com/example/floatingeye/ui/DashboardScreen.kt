package com.example.floatingeye.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.battery.BatteryColors
import com.example.floatingeye.battery.BatteryInfo
import com.example.floatingeye.battery.BatterySettingsRepository
import com.example.floatingeye.battery.FloatingEyeBatteryController
import com.example.floatingeye.controller.FloatingEyeController
import com.example.floatingeye.controller.FloatingEyeLightController
import com.example.floatingeye.gestures.GestureAppBinding
import com.example.floatingeye.gestures.GestureCaptureController
import com.example.floatingeye.gestures.GestureDrawingCanvas
import com.example.floatingeye.audio.BluetoothMicController
import com.example.floatingeye.ui.CyberBluetoothMicCard
import com.example.floatingeye.notifications.NotificationHistoryRepository
import com.example.floatingeye.ui.CyberNotificationHistoryCard
import com.example.floatingeye.gestures.GestureRecognizer
import com.example.floatingeye.gestures.GestureRepository
import com.example.floatingeye.gestures.GestureSetupFlowDialog
import com.example.floatingeye.gestures.GestureThumbnail
import com.example.floatingeye.gestures.RecognitionResult
import com.example.floatingeye.model.FloatingEyeConfig
import com.example.floatingeye.overlay.FloatingEyeOverlayService
import com.example.floatingeye.overlay.FloatingEyePositionManager
import com.example.floatingeye.overlay.OverlaySize
import com.example.floatingeye.ui.FloatingEyePlusWidget
import com.example.floatingeye.shortcuts.AppLaunchManager
import com.example.floatingeye.shortcuts.AppShortcut
import com.example.floatingeye.shortcuts.AppShortcutPicker
import com.example.floatingeye.shortcuts.AppShortcutRepository
import com.example.floatingeye.shortcuts.DrawableAppIcon
import com.example.floatingeye.shortcuts.InstalledAppsRepository
import com.example.floatingeye.shortcuts.RadialAppShortcutMenu
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.CyanPrimaryContainer
import com.example.ui.theme.StatusActiveCyan
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    positionManager: FloatingEyePositionManager,
    config: FloatingEyeConfig
) {
    val context = LocalContext.current
    val isOverlayRunning by FloatingEyeOverlayService.isServiceRunning.collectAsStateWithLifecycle()

    var selectedSize by remember { mutableStateOf(positionManager.getOverlaySize()) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    // App shortcuts architecture instances
    val shortcutRepository = remember { AppShortcutRepository(context) }
    val installedAppsRepository = remember { InstalledAppsRepository(context) }
    val launchManager = remember { AppLaunchManager(context, shortcutRepository) }
    val shortcuts by shortcutRepository.shortcuts.collectAsStateWithLifecycle()

    // Gestures architecture instances
    val gestureRepository = remember { GestureRepository(context) }
    val gestures by gestureRepository.gestures.collectAsStateWithLifecycle()
    var showGestureSetup by remember { mutableStateOf(false) }
    var editingGesture by remember { mutableStateOf<GestureAppBinding?>(null) }
    var changingAppGesture by remember { mutableStateOf<GestureAppBinding?>(null) }

    // Live Bluetooth Microphone architecture instance
    val micController = remember { BluetoothMicController.getInstance(context) }
    val isMicPlaying by micController.isMicPlaying.collectAsStateWithLifecycle()
    val micAudioLevel by micController.audioLevel.collectAsStateWithLifecycle()

    // Notification History & Media Vault repository
    val notificationRepo = remember { NotificationHistoryRepository.getInstance(context) }

    // Space-reduction subsystem filter: "ALL", "MIC", "NOTIF", "EYE"
    var dashboardSectionFilter by remember { mutableStateOf("ALL") }

    // Preview controllers for embedded 3D card
    val previewLightController = remember { FloatingEyeLightController(config.bloomEnabled, config.bloomStrength) }
    val previewEyeController = remember { FloatingEyeController(config, previewLightController) }
    var isPreviewRadialOpen by remember { mutableStateOf(false) }

    // Battery architecture instances
    val batterySettingsRepository = remember { BatterySettingsRepository(context) }
    val reactionsEnabled by batterySettingsRepository.reactionsEnabled.collectAsStateWithLifecycle()
    val lowBatteryEffects by batterySettingsRepository.lowBatteryEffects.collectAsStateWithLifecycle()
    val chargingAnimation by batterySettingsRepository.chargingAnimation.collectAsStateWithLifecycle()
    val fullChargeAnimation by batterySettingsRepository.fullChargeAnimation.collectAsStateWithLifecycle()

    val previewBatteryController = remember {
        FloatingEyeBatteryController(context, previewLightController, batterySettingsRepository)
    }
    val batteryInfo by previewBatteryController.batteryInfo.collectAsStateWithLifecycle()
    val drainMonitorEnabled by batterySettingsRepository.drainMonitorEnabled.collectAsStateWithLifecycle()
    val drainTelemetry by previewBatteryController.drainMonitor.telemetry.collectAsStateWithLifecycle()
    var selectedSimulatedState by remember { mutableStateOf("live") }

    DisposableEffect(Unit) {
        previewBatteryController.startListening()
        onDispose {
            previewBatteryController.stopListening()
        }
    }

    var expandedCardId by remember { mutableStateOf<String?>("gestures") }

    val bloomEnabled by previewLightController.bloomEnabled.collectAsStateWithLifecycle()
    val bloomStrength by previewLightController.bloomStrength.collectAsStateWithLifecycle()

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
                                text = "Hiris Controller",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            // Live Service status badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceContainerHigh
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
                                            .background(if (isOverlayRunning) CyanNeonPrimary else TextTertiary)
                                    )
                                    Text(
                                        text = if (isOverlayRunning) "OVERLAY ACTIVE" else "DISABLED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOverlayRunning) CyanNeonPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Control Dashboard",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    }
                },
                actions = {
                    // Quick Action: Live Bluetooth Mic status & toggle button in header
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMicPlaying) Color(0xFF003D20) else SurfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMicPlaying) Color(0xFF38E07B) else CyanNeonPrimary.copy(alpha = 0.5f)
                        ),
                        onClick = {
                            if (!micController.hasRecordPermission()) {
                                Toast.makeText(context, "Please grant microphone permission in the panel below", Toast.LENGTH_SHORT).show()
                            } else {
                                val active = micController.toggleStreaming()
                                Toast.makeText(
                                    context,
                                    if (active) "Live broadcast active (Bluetooth streaming)" else "Broadcast stopped",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("action_mic_header")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isMicPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                                contentDescription = "Quick Mic Toggle",
                                tint = if (isMicPlaying) Color(0xFF38E07B) else CyanNeonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isMicPlaying) "MIC ON" else "MIC OFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMicPlaying) Color(0xFF38E07B) else TextPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = { showInfoSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("action_info")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "System Info",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Primary Control Card: Floating Eye Enabled / Disabled
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(
                        listOf(
                            if (isOverlayRunning) CyanNeonPrimary.copy(alpha = 0.45f) else Color(0xFF222B38).copy(alpha = 0.4f),
                            Color(0xFF161E28).copy(alpha = 0.2f)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("primary_control_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isOverlayRunning) CyanPrimaryContainer else SurfaceContainerLow,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Visibility,
                                        contentDescription = null,
                                        tint = if (isOverlayRunning) CyanNeonPrimary else TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Hiris Controller",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isOverlayRunning) "Visible above Home Screen & apps" else "Overlay currently inactive",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isOverlayRunning) CyanNeonPrimary else TextSecondary
                                )
                            }
                        }

                        // Primary Switch (min 48dp touch area)
                        Switch(
                            checked = isOverlayRunning,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    FloatingEyeOverlayService.start(context)
                                } else {
                                    FloatingEyeOverlayService.stop(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanNeonPrimary,
                                checkedTrackColor = CyanPrimaryContainer,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = SurfaceContainerHigh
                            ),
                            modifier = Modifier.testTag("switch_floating_eye")
                        )
                    }

                    // Live Interactive Viewport with Draggable Plus Button & Radial Shortcuts
                    var plusOffsetX by remember { mutableFloatStateOf(0f) }
                    var plusOffsetY by remember { mutableFloatStateOf(0f) }
                    var isPreviewGestureActive by remember { mutableStateOf(false) }
                    var previewRecognizedResult by remember { mutableStateOf<RecognitionResult.Success?>(null) }
                    val previewGestureCapture = remember { GestureCaptureController() }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (isPreviewRadialOpen) {
                                        isPreviewRadialOpen = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPreviewGestureActive) {
                                // In-Card Gesture Drawing Mode
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (previewRecognizedResult != null) {
                                        val match = previewRecognizedResult!!
                                        // Futuristic Recognized Success Card (Does NOT close the app automatically!)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(SurfaceDark.copy(alpha = 0.95f))
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = SurfaceContainerHigh,
                                                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeonPrimary),
                                                modifier = Modifier.size(52.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    DrawableAppIcon(
                                                        drawable = remember(match.binding.packageName) {
                                                            installedAppsRepository.getAppIcon(match.binding.packageName)
                                                        },
                                                        contentDescription = match.binding.appName,
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Gesture Recognized: ${match.binding.appName}!",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = CyanNeonPrimary
                                            )
                                            Text(
                                                text = "Accuracy: ${(match.confidence * 100).toInt()}% match",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        previewRecognizedResult = null
                                                        previewGestureCapture.clear()
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.height(40.dp)
                                                ) {
                                                    Text("Draw Again", color = TextPrimary)
                                                }
                                                Button(
                                                    onClick = {
                                                        launchManager.launchApp(match.binding.packageName, match.binding.appName)
                                                        isPreviewGestureActive = false
                                                        previewRecognizedResult = null
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = CyanNeonPrimary,
                                                        contentColor = CyanOnPrimary
                                                    ),
                                                    modifier = Modifier.height(40.dp)
                                                ) {
                                                    Text("Launch App", color = CyanOnPrimary)
                                                }
                                            }
                                        }
                                    } else {
                                        GestureDrawingCanvas(
                                            captureController = previewGestureCapture,
                                            onPointerMove = { _, _ -> },
                                            onGestureComplete = {
                                                val points = previewGestureCapture.finishCapture()
                                                if (points.size < 4) {
                                                    previewGestureCapture.clear()
                                                    return@GestureDrawingCanvas
                                                }
                                                if (gestures.isEmpty() || gestures.none { it.isEnabled }) {
                                                    Toast.makeText(
                                                        context,
                                                        "No active gestures. Click \"+ Add Gesture\" below!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    isPreviewGestureActive = false
                                                    return@GestureDrawingCanvas
                                                }
                                                val result = GestureRecognizer.recognize(points, gestures)
                                                when (result) {
                                                    is RecognitionResult.Success -> {
                                                        previewRecognizedResult = result
                                                    }
                                                    is RecognitionResult.LowConfidence -> {
                                                        val hint = if (result.closest != null) " (Closest: ${result.closest.appName} ${(result.confidence * 100).toInt()}%)" else ""
                                                        Toast.makeText(context, "Gesture not recognized$hint. Try again!", Toast.LENGTH_SHORT).show()
                                                        previewGestureCapture.clear()
                                                    }
                                                    RecognitionResult.InsufficientPoints -> {
                                                        previewGestureCapture.clear()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SurfaceDark.copy(alpha = 0.9f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.5f)),
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = 10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(start = 10.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Draw your gesture here to test",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = CyanNeonPrimary
                                                )
                                                IconButton(
                                                    onClick = {
                                                        isPreviewGestureActive = false
                                                        previewRecognizedResult = null
                                                        previewGestureCapture.clear()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Cancel",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Normal Draggable Plus Button with Radial Shortcuts
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(plusOffsetX.roundToInt(), plusOffsetY.roundToInt()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Radial Shortcut Menu preview around the draggable Plus button
                                    RadialAppShortcutMenu(
                                        shortcuts = shortcuts,
                                        isOpen = isPreviewRadialOpen,
                                        isNearLeft = plusOffsetX < -50f,
                                        isNearRight = plusOffsetX > 50f,
                                        isMicPlaying = isMicPlaying,
                                        micAudioLevel = micAudioLevel,
                                        onToggleMic = {
                                            if (!micController.hasRecordPermission()) {
                                                Toast.makeText(context, "Please grant microphone permission in the card below to broadcast!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val active = micController.toggleStreaming()
                                                Toast.makeText(
                                                    context,
                                                    if (active) "Live broadcast active (Bluetooth streaming)" else "Broadcast stopped",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onLaunchShortcut = { sc ->
                                            launchManager.launchApp(sc.packageName, sc.displayName)
                                            isPreviewRadialOpen = false
                                        }
                                    )

                                    // Floating Plus Button: Tap to open, Drag to move, Long-press for gesture
                                    val previewNeonColor by previewLightController.neonColor.collectAsStateWithLifecycle()
                                    val previewIrisColor by previewLightController.irisColor.collectAsStateWithLifecycle()
                                    val previewEmissivePulse by previewLightController.emissivePulse.collectAsStateWithLifecycle()
                                    val previewGlowIntensity by previewLightController.glowIntensity.collectAsStateWithLifecycle()

                                    Box(
                                        modifier = Modifier
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onTap = {
                                                        isPreviewRadialOpen = !isPreviewRadialOpen
                                                    },
                                                    onLongPress = {
                                                        isPreviewRadialOpen = false
                                                        previewGestureCapture.startCapture()
                                                        isPreviewGestureActive = true
                                                    }
                                                )
                                            }
                                            .pointerInput(Unit) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        isPreviewRadialOpen = false
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        plusOffsetX = (plusOffsetX + dragAmount.x).coerceIn(-130f, 130f)
                                                        plusOffsetY = (plusOffsetY + dragAmount.y).coerceIn(-75f, 75f)
                                                    }
                                                )
                                            }
                                    ) {
                                        FloatingEyePlusWidget(
                                            size = 64.dp,
                                            isOpen = isPreviewRadialOpen,
                                            isCharging = batteryInfo.isCharging,
                                            isFull = batteryInfo.isFull,
                                            batteryPercentage = batteryInfo.percentage,
                                            neonColor = previewNeonColor,
                                            irisColor = previewIrisColor,
                                            emissivePulse = previewEmissivePulse,
                                            glowIntensity = previewGlowIntensity
                                        )
                                    }
                                }

                                // Interactive Hint pill at bottom
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceDark.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = if (isPreviewRadialOpen) "Tap top mic icon to broadcast • Tap app to launch" else "Tap (+) for apps & live mic • Drag to move",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isPreviewRadialOpen) CyanNeonPrimary else TextSecondary
                                        )
                                        if (plusOffsetX != 0f || plusOffsetY != 0f) {
                                            Text(
                                                text = "Reset",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CyanNeonPrimary,
                                                modifier = Modifier.clickable {
                                                    plusOffsetX = 0f
                                                    plusOffsetY = 0f
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Direct Quick Access to Add Apps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Shortcuts (${shortcuts.size}/${AppShortcutRepository.MAX_SHORTCUTS})",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Opens on 1 click of (+) button",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeonPrimary
                            )
                        }

                        Button(
                            onClick = { showAppPicker = true },
                            enabled = shortcuts.size < AppShortcutRepository.MAX_SHORTCUTS,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeonPrimary,
                                contentColor = CyanOnPrimary,
                                disabledContainerColor = SurfaceContainerHigh,
                                disabledContentColor = TextTertiary
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add App",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Text(
                        text = if (isOverlayRunning)
                            "Plus button is floating over all apps! Tap it to open apps, long-press to draw gestures, or drag it freely."
                        else
                            "Turn ON the switch to spawn the Floating Plus button above your apps and home screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Prominent Dedicated Live Bluetooth Megaphone / Mic Section (Always visible)
            CyberBluetoothMicCard(
                micController = micController,
                modifier = Modifier.fillMaxWidth()
            )

            // Notification & Media Vault Section (Capture, Preserved Deleted Messages/Photos, 8h/24h/1semaine auto-delete)
            CyberNotificationHistoryCard(
                repository = notificationRepo,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Configuration & Subsystems",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // Section 1: Gestures Section (Customizable gesture-to-app system)
            NavigationFeatureCard(
                icon = Icons.Rounded.TouchApp,
                title = "Gestures",
                subtitle = "Draw-to-launch actions (${gestures.size} saved)",
                isExpanded = expandedCardId == "gestures",
                onClick = {
                    expandedCardId = if (expandedCardId == "gestures") null else "gestures"
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gesture Bindings",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )

                        // + Add Gesture button (min 48dp touch target)
                        Button(
                            onClick = {
                                editingGesture = null
                                showGestureSetup = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeonPrimary,
                                contentColor = CyanOnPrimary
                            ),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("button_add_gesture")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Gesture",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    if (gestures.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Gesture,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No custom gestures configured",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Press '+ Add Gesture' to draw a symbol and link it to any app on your phone.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            gestures.forEach { binding ->
                                GestureRowItem(
                                    binding = binding,
                                    drawable = remember(binding.packageName) {
                                        installedAppsRepository.getAppIcon(binding.packageName)
                                    },
                                    onToggle = {
                                        gestureRepository.toggleEnabled(binding.id, !binding.isEnabled)
                                    },
                                    onRedraw = {
                                        editingGesture = binding
                                        showGestureSetup = true
                                    },
                                    onChangeApp = {
                                        changingAppGesture = binding
                                    },
                                    onDelete = {
                                        gestureRepository.deleteGesture(binding.id)
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "How Gesture Activation Works:",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeonPrimary
                            )
                            Text(
                                text = "1. Press and hold the Floating Eye\n2. Eye emits a cyan pulse\n3. Draw your symbol anywhere without lifting\n4. Release to automatically open the linked app",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Section 2: Shortcuts Section (Customizable radial app shortcuts)
            NavigationFeatureCard(
                icon = Icons.Rounded.Widgets,
                title = "Shortcuts",
                subtitle = "Radial app menu around Floating Eye (${shortcuts.size}/${AppShortcutRepository.MAX_SHORTCUTS})",
                isExpanded = expandedCardId == "shortcuts",
                onClick = {
                    expandedCardId = if (expandedCardId == "shortcuts") null else "shortcuts"
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected Apps (Max ${AppShortcutRepository.MAX_SHORTCUTS})",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )

                        // Add App button (min 48dp touch target)
                        Button(
                            onClick = { showAppPicker = true },
                            enabled = shortcuts.size < AppShortcutRepository.MAX_SHORTCUTS,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeonPrimary,
                                contentColor = CyanOnPrimary,
                                disabledContainerColor = SurfaceContainerHigh,
                                disabledContentColor = TextTertiary
                            ),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("button_add_app")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add App",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    if (shortcuts.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Apps,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No shortcuts configured yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Press 'Add App' to select installed apps to fan out around the Floating Eye on tap.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            shortcuts.forEachIndexed { index, shortcut ->
                                ShortcutRowItem(
                                    shortcut = shortcut,
                                    index = index,
                                    totalCount = shortcuts.size,
                                    drawable = remember(shortcut.packageName) {
                                        installedAppsRepository.getAppIcon(shortcut.packageName)
                                    },
                                    onMoveUp = { shortcutRepository.moveUp(index) },
                                    onMoveDown = { shortcutRepository.moveDown(index) },
                                    onRemove = { shortcutRepository.removeShortcut(shortcut.packageName) },
                                    onTestLaunch = {
                                        launchManager.launchApp(shortcut.packageName, shortcut.displayName)
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "• Tapping the Floating Eye displays shortcuts in a curved radial arc\n• Automatically fans toward the screen center to avoid clipping\n• App names are shown only on press or focus",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            // Section 3: Appearance Card
            NavigationFeatureCard(
                icon = Icons.Rounded.Palette,
                title = "Appearance",
                subtitle = "Overlay size presets and cyan bloom luminosity",
                isExpanded = expandedCardId == "appearance",
                onClick = {
                    expandedCardId = if (expandedCardId == "appearance") null else "appearance"
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Overlay Size Presets",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OverlaySize.values().forEach { size ->
                            FilterChip(
                                selected = selectedSize == size,
                                onClick = {
                                    selectedSize = size
                                    positionManager.saveOverlaySize(size)
                                    if (isOverlayRunning) {
                                        FloatingEyeOverlayService.updateSize(context, size)
                                    }
                                },
                                label = { Text(size.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimaryContainer,
                                    selectedLabelColor = CyanNeonPrimary,
                                    containerColor = SurfaceContainerLow,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    // Bloom Controls
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
                                text = "FE_NeonCyan Bloom",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        Switch(
                            checked = bloomEnabled,
                            onCheckedChange = { previewLightController.setBloomEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanNeonPrimary,
                                checkedTrackColor = CyanPrimaryContainer
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
                                text = "Bloom Strength",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(90.dp)
                            )
                            Slider(
                                value = bloomStrength,
                                onValueChange = { previewLightController.setBloomStrength(it) },
                                valueRange = 0.1f..1.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanNeonPrimary,
                                    activeTrackColor = CyanNeonPrimary
                                )
                            )
                            Text(
                                text = String.format("%.2f", bloomStrength),
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeonPrimary
                            )
                        }
                    }
                }
            }

            // Section 4: Position Card
            NavigationFeatureCard(
                icon = Icons.Rounded.OpenWith,
                title = "Position",
                subtitle = "Free drag placement and coordinate persistence",
                isExpanded = expandedCardId == "position",
                onClick = {
                    expandedCardId = if (expandedCardId == "position") null else "position"
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "• Drag the Floating Eye freely to any screen location\n• Released position is saved and restored automatically\n• Does not snap to edges, giving complete placement freedom",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val metrics = context.resources.displayMetrics
                                val defaultX = (metrics.widthPixels - 200).coerceAtLeast(0)
                                val defaultY = (metrics.heightPixels * 0.2f).toInt()
                                positionManager.savePosition(defaultX, defaultY)
                                if (isOverlayRunning) {
                                    FloatingEyeOverlayService.stop(context)
                                    FloatingEyeOverlayService.start(context)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CenterFocusStrong,
                                contentDescription = null,
                                tint = CyanNeonPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reset Position to Top-Right",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Section 5: Battery Reactions Card
            NavigationFeatureCard(
                icon = Icons.Rounded.BatteryChargingFull,
                title = "Battery Reactions",
                subtitle = "Luminous charging waves & battery health glows",
                isExpanded = expandedCardId == "battery",
                onClick = {
                    expandedCardId = if (expandedCardId == "battery") null else "battery"
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Current Device Battery Status Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (batteryInfo.isCharging) BatteryColors.NeonElectricCyan.copy(alpha = 0.5f)
                            else when (batteryInfo.healthState) {
                                com.example.floatingeye.battery.BatteryHealthState.NORMAL -> BatteryColors.NeonCyanNormal.copy(alpha = 0.4f)
                                com.example.floatingeye.battery.BatteryHealthState.LOW -> BatteryColors.NeonAmberLow.copy(alpha = 0.5f)
                                com.example.floatingeye.battery.BatteryHealthState.CRITICAL,
                                com.example.floatingeye.battery.BatteryHealthState.VERY_CRITICAL -> BatteryColors.NeonRedCritical.copy(alpha = 0.5f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SurfaceContainerLow,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (batteryInfo.isCharging) Icons.Rounded.Bolt
                                        else when (batteryInfo.healthState) {
                                            com.example.floatingeye.battery.BatteryHealthState.NORMAL -> Icons.Rounded.BatteryStd
                                            com.example.floatingeye.battery.BatteryHealthState.LOW -> Icons.Rounded.BatteryAlert
                                            com.example.floatingeye.battery.BatteryHealthState.CRITICAL,
                                            com.example.floatingeye.battery.BatteryHealthState.VERY_CRITICAL -> Icons.Rounded.BatteryAlert
                                        },
                                        contentDescription = null,
                                        tint = if (batteryInfo.isCharging) BatteryColors.NeonElectricCyan
                                        else when (batteryInfo.healthState) {
                                            com.example.floatingeye.battery.BatteryHealthState.NORMAL -> BatteryColors.NeonCyanNormal
                                            com.example.floatingeye.battery.BatteryHealthState.LOW -> BatteryColors.NeonAmberLow
                                            com.example.floatingeye.battery.BatteryHealthState.CRITICAL,
                                            com.example.floatingeye.battery.BatteryHealthState.VERY_CRITICAL -> BatteryColors.NeonRedCritical
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (batteryInfo.isFull) "Fully Charged (100%)"
                                    else if (batteryInfo.isCharging) "Charging (${batteryInfo.percentage}%)"
                                    else "Battery Level: ${batteryInfo.percentage}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (batteryInfo.isFull) "Cyan + subtle green accent (#38E07B)"
                                    else if (batteryInfo.isCharging) "Electric cyan (#00E5FF) pulse active"
                                    else when (batteryInfo.healthState) {
                                        com.example.floatingeye.battery.BatteryHealthState.NORMAL -> "Normal State: Cyan (#00CCFF), Blue Iris"
                                        com.example.floatingeye.battery.BatteryHealthState.LOW -> "Low Battery: Amber (#FFB020) slow pulse"
                                        com.example.floatingeye.battery.BatteryHealthState.CRITICAL -> "Critical: Red (#FF3B30) warning pulse"
                                        com.example.floatingeye.battery.BatteryHealthState.VERY_CRITICAL -> "Very Critical: Darkened subtle red"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Setting 1: Battery Reactions Master On/Off
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Battery Reactions",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Automatically reflect phone battery & charging states on the Floating Eye",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = reactionsEnabled,
                            onCheckedChange = {
                                batterySettingsRepository.setReactionsEnabled(it)
                                previewBatteryController.applyCurrentState()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanNeonPrimary,
                                checkedTrackColor = CyanPrimaryContainer
                            )
                        )
                    }

                    // Setting 2: Low Battery Effects On/Off
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Low Battery Effects",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Amber (#FFB020) and Red (#FF3B30) pulses when battery is under 30%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = lowBatteryEffects,
                            enabled = reactionsEnabled,
                            onCheckedChange = {
                                batterySettingsRepository.setLowBatteryEffects(it)
                                previewBatteryController.applyCurrentState()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanNeonPrimary,
                                checkedTrackColor = CyanPrimaryContainer
                            )
                        )
                    }

                    // Setting 3: Charging Animation On/Off
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Charging Animation",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Sequential light wave through 6 neon parts upon connecting charger",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = chargingAnimation,
                            enabled = reactionsEnabled,
                            onCheckedChange = {
                                batterySettingsRepository.setChargingAnimation(it)
                                previewBatteryController.applyCurrentState()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanNeonPrimary,
                                checkedTrackColor = CyanPrimaryContainer
                            )
                        )
                    }

                    // Setting 4: Full Charge Animation On/Off
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Full Charge Animation",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Short green accent celebration pulse when reaching 100%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = fullChargeAnimation,
                            enabled = reactionsEnabled,
                            onCheckedChange = {
                                batterySettingsRepository.setFullChargeAnimation(it)
                                previewBatteryController.applyCurrentState()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanNeonPrimary,
                                checkedTrackColor = CyanPrimaryContainer
                            )
                        )
                    }

                    // Interactive Live State Simulator
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Interactive State Simulator",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap a state to preview lighting and pulses in the embedded 3D viewport above:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedSimulatedState == "live",
                                onClick = {
                                    selectedSimulatedState = "live"
                                    previewBatteryController.setSimulationOverride(null)
                                },
                                label = { Text("Live Device") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimaryContainer,
                                    selectedLabelColor = CyanNeonPrimary,
                                    containerColor = SurfaceContainerLow,
                                    labelColor = TextSecondary
                                )
                            )
                            FilterChip(
                                selected = selectedSimulatedState == "normal",
                                onClick = {
                                    selectedSimulatedState = "normal"
                                    previewBatteryController.setSimulationOverride(
                                        BatteryInfo(85, isCharging = false, isFull = false)
                                    )
                                },
                                label = { Text("Normal (85%)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimaryContainer,
                                    selectedLabelColor = CyanNeonPrimary,
                                    containerColor = SurfaceContainerLow,
                                    labelColor = TextSecondary
                                )
                            )
                            FilterChip(
                                selected = selectedSimulatedState == "low",
                                onClick = {
                                    selectedSimulatedState = "low"
                                    previewBatteryController.setSimulationOverride(
                                        BatteryInfo(20, isCharging = false, isFull = false)
                                    )
                                },
                                label = { Text("Low (20%)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF45300B),
                                    selectedLabelColor = BatteryColors.NeonAmberLow,
                                    containerColor = SurfaceContainerLow,
                                    labelColor = TextSecondary
                                )
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedSimulatedState == "critical",
                                onClick = {
                                    selectedSimulatedState = "critical"
                                    previewBatteryController.setSimulationOverride(
                                        BatteryInfo(10, isCharging = false, isFull = false)
                                    )
                                },
                                label = { Text("Critical (10%)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF45100B),
                                    selectedLabelColor = BatteryColors.NeonRedCritical,
                                    containerColor = SurfaceContainerLow,
                                    labelColor = TextSecondary
                                )
                            )
                            FilterChip(
                                selected = selectedSimulatedState == "charging",
                                onClick = {
                                    selectedSimulatedState = "charging"
                                    previewBatteryController.setSimulationOverride(
                                        BatteryInfo(55, isCharging = true, isFull = false)
                                    )
                                },
                                label = { Text("Charging (55%)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimaryContainer,
                                    selectedLabelColor = BatteryColors.NeonElectricCyan,
                                    containerColor = SurfaceContainerLow,
                                    labelColor = TextSecondary
                                )
                            )
                            FilterChip(
                                selected = selectedSimulatedState == "full",
                                onClick = {
                                    selectedSimulatedState = "full"
                                    previewBatteryController.setSimulationOverride(
                                        BatteryInfo(100, isCharging = true, isFull = true)
                                    )
                                },
                                label = { Text("Full (100%)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0F3A22),
                                    selectedLabelColor = BatteryColors.GreenAccent,
                                    containerColor = SurfaceContainerLow,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Section 6: Real-time Battery Drain & Power Telemetry (OFF by default)
            CyberPowerDrainCard(
                isEnabled = drainMonitorEnabled,
                telemetry = drainTelemetry,
                settingsRepository = batterySettingsRepository
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // App Picker Sheet for Shortcuts
    if (showAppPicker) {
        AppShortcutPicker(
            installedAppsRepository = installedAppsRepository,
            selectedShortcuts = shortcuts,
            onToggleApp = { appInfo, isSelected ->
                if (isSelected) {
                    shortcutRepository.addShortcut(
                        AppShortcut(
                            packageName = appInfo.packageName,
                            displayName = appInfo.displayName
                        )
                    )
                } else {
                    shortcutRepository.removeShortcut(appInfo.packageName)
                }
            },
            onDismiss = { showAppPicker = false }
        )
    }

    // Gesture Setup Flow Dialog
    if (showGestureSetup) {
        GestureSetupFlowDialog(
            installedAppsRepository = installedAppsRepository,
            gestureRepository = gestureRepository,
            config = config,
            editingBinding = editingGesture,
            onDismiss = {
                showGestureSetup = false
                editingGesture = null
            },
            onCompleted = { savedBinding ->
                showGestureSetup = false
                editingGesture = null
                Toast.makeText(
                    context,
                    "Gesture for \"${savedBinding.appName}\" saved and ready to use!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // Change App Sheet for existing gesture
    changingAppGesture?.let { binding ->
        AppShortcutPicker(
            installedAppsRepository = installedAppsRepository,
            selectedShortcuts = emptyList(),
            onToggleApp = { newApp, _ ->
                val updated = binding.copy(
                    packageName = newApp.packageName,
                    appName = newApp.displayName
                )
                gestureRepository.saveGesture(updated)
                changingAppGesture = null
            },
            onDismiss = { changingAppGesture = null }
        )
    }

    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SurfaceContainerHigh,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Floating Eye Architecture & Gestures",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "• Model: FloatingEye.glb (binary glTF 2.0)\n• Renderer: SceneView / Filament 3D Engine\n• Gestures: Geometric path normalization & $1 Recognizer algorithm\n• Activation: Press & hold eye -> Cyan pulse -> Fullscreen draw -> Release",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Clean Row item representing a saved gesture binding with gesture thumbnail,
 * app info, enable/disable switch, redraw, change app, and delete controls.
 */
@Composable
private fun GestureRowItem(
    binding: GestureAppBinding,
    drawable: android.graphics.drawable.Drawable?,
    onToggle: () -> Unit,
    onRedraw: () -> Unit,
    onChangeApp: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gesture_row_${binding.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // [gesture preview] thumbnail (48dp)
            GestureThumbnail(
                normalizedPoints = binding.normalizedPoints,
                size = 48.dp
            )

            // App Icon: 32dp
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceContainerHigh,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DrawableAppIcon(
                        drawable = drawable,
                        contentDescription = binding.appName,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }
            }

            // App title & info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = binding.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (binding.isEnabled) TextPrimary else TextTertiary,
                    maxLines = 1
                )
                Text(
                    text = if (binding.isEnabled) "Active" else "Disabled",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (binding.isEnabled) CyanNeonPrimary else TextTertiary
                )
            }

            // Redraw button
            IconButton(
                onClick = onRedraw,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Redraw Gesture",
                    tint = CyanNeonPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Change App button
            IconButton(
                onClick = onChangeApp,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = "Change App",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete Gesture",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Enable / Disable switch
            Switch(
                checked = binding.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyanNeonPrimary,
                    checkedTrackColor = CyanPrimaryContainer,
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = SurfaceContainerHigh
                )
            )
        }
    }
}

/**
 * Clean Row item representing an added App Shortcut with Reorder & Remove controls.
 */
@Composable
private fun ShortcutRowItem(
    shortcut: AppShortcut,
    index: Int,
    totalCount: Int,
    drawable: android.graphics.drawable.Drawable?,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onTestLaunch: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shortcut_item_${shortcut.packageName}")
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Container 50dp, App Icon 30dp
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerHigh,
                modifier = Modifier
                    .size(50.dp)
                    .clickable { onTestLaunch() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DrawableAppIcon(
                        drawable = drawable,
                        contentDescription = shortcut.displayName,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTestLaunch() }
            ) {
                Text(
                    text = shortcut.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = shortcut.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    maxLines = 1
                )
            }

            // Move Up button
            IconButton(
                onClick = onMoveUp,
                enabled = index > 0,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    tint = if (index > 0) CyanNeonPrimary else TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Move Down button
            IconButton(
                onClick = onMoveDown,
                enabled = index < totalCount - 1,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    tint = if (index < totalCount - 1) CyanNeonPrimary else TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove Shortcut",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Standard reusable navigation card for dashboard feature modules.
 * Adheres strictly to 16-20dp rounded containers, 22-24dp icons, 48dp touch targets.
 */
@Composable
private fun NavigationFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SurfaceContainerLow,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = CyanNeonPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    expandedContent()
                }
            }
        }
    }
}
