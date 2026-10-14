package com.example.floatingeye.ui.extensions

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.ui.graphics.Brush
import com.example.floatingeye.downloader.repository.MediaDownloadRepository
import com.example.floatingeye.notifications.NotificationPermissionHelper
import com.example.floatingeye.tasks.repository.TaskRepository
import com.example.floatingeye.ui.downloader.CyberMediaDownloaderCard
import com.example.floatingeye.ui.tasks.CyberTaskManagerCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.audio.BluetoothMicController
import com.example.floatingeye.battery.BatterySettingsRepository
import com.example.floatingeye.battery.FloatingEyeBatteryController
import com.example.floatingeye.extensions.model.HirisExtension
import com.example.floatingeye.extensions.repository.ExtensionRepository
import com.example.floatingeye.gestures.GestureRepository
import com.example.floatingeye.gestures.GestureThumbnail
import com.example.floatingeye.notifications.NotificationHistoryRepository
import com.example.floatingeye.notifications.CapturedNotification
import com.example.floatingeye.notifications.RetentionPeriod
import com.example.floatingeye.overlay.FloatingEyePositionManager
import com.example.floatingeye.overlay.OverlaySize
import com.example.floatingeye.shortcuts.AppShortcutRepository
import com.example.floatingeye.shortcuts.DrawableAppIcon
import com.example.floatingeye.shortcuts.InstalledAppsRepository
import com.example.floatingeye.ui.CyberBluetoothMicCard
import com.example.floatingeye.ui.CyberNotificationHistoryCard
import com.example.floatingeye.ui.CyberPowerDrainCard
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionConfigSheet(
    extension: HirisExtension,
    onDismiss: () -> Unit,
    shortcutRepository: AppShortcutRepository,
    installedAppsRepository: InstalledAppsRepository,
    gestureRepository: GestureRepository,
    batterySettingsRepository: BatterySettingsRepository,
    batteryController: FloatingEyeBatteryController,
    micController: BluetoothMicController,
    notificationRepository: NotificationHistoryRepository,
    positionManager: FloatingEyePositionManager,
    taskRepository: TaskRepository? = null,
    onReminderRequested: ((CapturedNotification) -> Unit)? = null,
    mediaDownloadRepository: MediaDownloadRepository? = null,
    onFirewallActiveChanged: (Boolean) -> Unit,
    onRequestFirewallEnable: () -> Unit,
    onAddShortcutClick: () -> Unit,
    onAddGestureClick: () -> Unit,
    onSimulateBatteryState: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0B111D),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF23324C))
            )
        }
    ) {
        val context = LocalContext.current
        val effectiveTaskRepo = remember(context, taskRepository) {
            taskRepository ?: TaskRepository.getInstance(context)
        }
        val effectiveDownloadRepo = remember(context, mediaDownloadRepository) {
            mediaDownloadRepository ?: MediaDownloadRepository.getInstance(context)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${extension.name} Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = extension.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF152236))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (extension.id) {
                ExtensionRepository.ID_SHORTCUTS -> {
                    ShortcutsConfigContent(
                        shortcutRepository = shortcutRepository,
                        onAddShortcutClick = onAddShortcutClick
                    )
                }
                ExtensionRepository.ID_GESTURES -> {
                    GesturesConfigContent(
                        gestureRepository = gestureRepository,
                        onAddGestureClick = onAddGestureClick
                    )
                }
                ExtensionRepository.ID_BATTERY -> {
                    BatteryReactionsConfigContent(
                        batterySettingsRepository = batterySettingsRepository,
                        batteryController = batteryController,
                        onSimulateBatteryState = onSimulateBatteryState
                    )
                }
                ExtensionRepository.ID_DRAIN_MONITOR -> {
                    val isDrainEnabled by batterySettingsRepository.drainMonitorEnabled.collectAsStateWithLifecycle()
                    val drainTelemetry by batteryController.drainMonitor.telemetry.collectAsStateWithLifecycle()
                    val context = LocalContext.current

                    CyberPowerDrainCard(
                        isEnabled = isDrainEnabled,
                        telemetry = drainTelemetry,
                        settingsRepository = batterySettingsRepository,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ExtensionRepository.ID_APPEARANCE -> {
                    AppearanceExtensionSettings(positionManager = positionManager)
                }
                ExtensionRepository.ID_BLUETOOTH_MIC -> {
                    CyberBluetoothMicCard(
                        micController = micController,
                        initiallyExpanded = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ExtensionRepository.ID_NOTIFICATIONS -> {
                    CyberNotificationHistoryCard(
                        repository = notificationRepository,
                        initiallyExpanded = true,
                        modifier = Modifier.fillMaxWidth(),
                        onCreateReminder = onReminderRequested
                    )
                }
                ExtensionRepository.ID_TASKS -> {
                    CyberTaskManagerCard(
                        repository = effectiveTaskRepo,
                        initiallyExpanded = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ExtensionRepository.ID_DOWNLOADER -> {
                    CyberMediaDownloaderCard(
                        repository = effectiveDownloadRepo,
                        initiallyExpanded = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ExtensionRepository.ID_FIREWALL -> {
                    val firewallRepo = remember(context) {
                        com.example.floatingeye.firewall.repository.FirewallRepository.getInstance(context)
                    }
                    com.example.floatingeye.ui.firewall.CyberFirewallCard(
                        repository = firewallRepo,
                        installedAppsRepository = installedAppsRepository,
                        onFirewallActiveChanged = onFirewallActiveChanged,
                        onRequestFirewallEnable = onRequestFirewallEnable,
                        initiallyExpanded = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    Text(
                        text = "Configuration options for this extension will appear here.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// 1. Shortcuts Config
@Composable
private fun ShortcutsConfigContent(
    shortcutRepository: AppShortcutRepository,
    onAddShortcutClick: () -> Unit
) {
    val shortcuts by shortcutRepository.shortcuts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Configured Shortcuts (${shortcuts.size}/6)",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            if (shortcuts.size < 6) {
                Button(
                    onClick = onAddShortcutClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeonPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add",
                        tint = CyanOnPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Shortcut", color = CyanOnPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (shortcuts.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF101828),
                border = BorderStroke(1.dp, Color(0xFF1C2A44)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No shortcuts added yet. Tap \"Add Shortcut\" to choose apps to display around HIRIS.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            shortcuts.forEach { shortcut ->
                val appIcon = remember(shortcut.packageName) {
                    try {
                        context.packageManager.getApplicationIcon(shortcut.packageName)
                    } catch (_: Exception) {
                        null
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF101828),
                    border = BorderStroke(1.dp, Color(0xFF1D2C46)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawableAppIcon(
                            drawable = appIcon,
                            contentDescription = shortcut.displayName,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shortcut.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = shortcut.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = { shortcutRepository.removeShortcut(shortcut.packageName) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Remove",
                                tint = Color(0xFFF87171)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 2. Gestures Config
@Composable
fun GesturesConfigContent(
    gestureRepository: GestureRepository,
    onAddGestureClick: () -> Unit
) {
    val gestures by gestureRepository.gestures.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saved Gestures (${gestures.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            Button(
                onClick = onAddGestureClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeonPrimary)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "New Gesture",
                    tint = CyanOnPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Gesture", color = CyanOnPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (gestures.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF101828),
                border = BorderStroke(1.dp, Color(0xFF1C2A44)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No gestures recorded yet. Create gestures like \"C\" for Chrome or \"W\" for WhatsApp to launch them from HIRIS.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            gestures.forEach { binding ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF101828),
                    border = BorderStroke(1.dp, Color(0xFF1D2C46)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GestureThumbnail(
                            normalizedPoints = binding.normalizedPoints,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = binding.appName,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "${binding.normalizedPoints.size} points recorded",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                        Switch(
                            checked = binding.isEnabled,
                            onCheckedChange = { enabled ->
                                gestureRepository.toggleEnabled(binding.id, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyanNeonPrimary
                            )
                        )
                        IconButton(
                            onClick = { gestureRepository.deleteGesture(binding.id) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFF87171)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 3. Battery Reactions Config
@Composable
fun BatteryReactionsConfigContent(
    batterySettingsRepository: BatterySettingsRepository,
    batteryController: FloatingEyeBatteryController,
    onSimulateBatteryState: (String) -> Unit
) {
    val reactionsEnabled by batterySettingsRepository.reactionsEnabled.collectAsStateWithLifecycle()
    val chargingAnim by batterySettingsRepository.chargingAnimation.collectAsStateWithLifecycle()
    val fullChargeAnim by batterySettingsRepository.fullChargeAnimation.collectAsStateWithLifecycle()
    val lowBatteryEffects by batterySettingsRepository.lowBatteryEffects.collectAsStateWithLifecycle()
    val batteryInfo by batteryController.batteryInfo.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Battery status badge
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF10192A),
            border = BorderStroke(1.dp, Color(0xFF1E2D48)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryChargingFull,
                    contentDescription = null,
                    tint = CyanNeonPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Device Battery: ${batteryInfo.percentage}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (batteryInfo.isCharging) "Charging active • Plasma reactor spinning" else "Discharging • Normal operation",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (batteryInfo.isCharging) CyanNeonPrimary else TextSecondary
                    )
                }
            }
        }

        // Toggles
        SettingSwitchRow(
            title = "Charging Plasma Ray",
            subtitle = "Orbiting 360° electric cyan beam while plugged in",
            checked = chargingAnim,
            onCheckedChange = { batterySettingsRepository.setChargingAnimation(it) }
        )

        SettingSwitchRow(
            title = "Full Charge Emerald Glow",
            subtitle = "Vivid green reactor celebration at 100%",
            checked = fullChargeAnim,
            onCheckedChange = { batterySettingsRepository.setFullChargeAnimation(it) }
        )

        SettingSwitchRow(
            title = "Low Battery Pulse Alert",
            subtitle = "Amber/Red warning heartbeat below 20%",
            checked = lowBatteryEffects,
            onCheckedChange = { batterySettingsRepository.setLowBatteryEffects(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Test Animations (Simulator)",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val states = listOf(
                "live" to "Live Device",
                "charging" to "Charging (55%)",
                "full" to "Full (100%)",
                "low" to "Low (20%)",
                "critical" to "Critical (10%)"
            )
            items(states) { (key, label) ->
                OutlinedButton(
                    onClick = { onSimulateBatteryState(key) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E2E4A))
                ) {
                    Text(label, fontSize = 12.sp, color = CyanNeonPrimary)
                }
            }
        }
    }
}

// 4. Appearance Config
@Composable
fun AppearanceExtensionSettings(
    positionManager: FloatingEyePositionManager
) {
    val context = LocalContext.current
    var selectedSize by remember { mutableStateOf(positionManager.getOverlaySize()) }
    var bloomStrength by remember { mutableFloatStateOf(positionManager.getBloomStrength()) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Floating Widget Size", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverlaySize.values().forEach { size ->
                val selected = selectedSize == size
                Surface(
                    onClick = {
                        selectedSize = size
                        positionManager.saveOverlaySize(size)
                        if (com.example.floatingeye.overlay.FloatingEyeOverlayService.isServiceRunning.value) {
                            com.example.floatingeye.overlay.FloatingEyeOverlayService.updateSize(context, size)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) Color(0xFF142742) else Color(0xFF101828),
                    border = BorderStroke(1.dp, if (selected) CyanNeonPrimary else Color(0xFF1C2B44)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = size.name.take(3),
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) CyanNeonPrimary else TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${size.sizeDp.toInt()}dp",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text("Glow & Bloom Intensity", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Slider(
            value = bloomStrength,
            onValueChange = { bloomStrength = it },
            onValueChangeFinished = {
                positionManager.saveBloomStrength(bloomStrength)
                com.example.floatingeye.overlay.FloatingEyeOverlayService.updateBloomStrength(context, bloomStrength)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = CyanNeonPrimary,
                activeTrackColor = CyanNeonPrimary,
                inactiveTrackColor = Color(0xFF1B273A)
            )
        )
    }
}

// 5. Bluetooth Mic Config
@Composable
private fun BluetoothMicConfigContent(micController: BluetoothMicController) {
    val isPlaying by micController.isMicPlaying.collectAsStateWithLifecycle()
    val audioLevel by micController.audioLevel.collectAsStateWithLifecycle()
    val isBtConnected by micController.isBluetoothConnected.collectAsStateWithLifecycle()
    val connectedDevice by micController.connectedDeviceName.collectAsStateWithLifecycle()
    val gain by micController.micGain.collectAsStateWithLifecycle()
    val isMuted by micController.isMuted.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Status & Route Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF10192A),
            border = BorderStroke(1.dp, Color(0xFF1E2D48)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Audio Output Target", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(connectedDevice, style = MaterialTheme.typography.bodySmall, color = CyanNeonPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBtConnected) Color(0xFF0F2F1C) else Color(0xFF182236),
                        border = BorderStroke(1.dp, if (isBtConnected) Color(0xFF22C55E) else Color(0xFF28364C))
                    ) {
                        Text(
                            text = if (isBtConnected) "BT Connected" else "Internal Route",
                            color = if (isBtConnected) Color(0xFF22C55E) else TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Audio VU Level Meter
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Live Mic Level", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        Text(if (isPlaying) "${(audioLevel * 100).toInt()}%" else "Inactive", style = MaterialTheme.typography.labelSmall, color = if (isPlaying) CyanNeonPrimary else TextTertiary)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0A0F1A))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = if (isPlaying) audioLevel.coerceIn(0.04f, 1f) else 0f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(CyanNeonPrimary, Color(0xFF38E07B))
                                    )
                                )
                        )
                    }
                }
            }
        }

        // Gain Slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mic Pre-Gain Boost", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("${String.format("%.1f", gain)}x", style = MaterialTheme.typography.bodySmall, color = CyanNeonPrimary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = gain,
                onValueChange = { micController.setGain(it) },
                valueRange = 0.5f..3.0f,
                colors = SliderDefaults.colors(
                    thumbColor = CyanNeonPrimary,
                    activeTrackColor = CyanNeonPrimary,
                    inactiveTrackColor = Color(0xFF1B273A)
                )
            )
        }

        // Quick Controls Row (Mute & Refresh)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { micController.toggleMute() },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isMuted) Color(0xFFEF4444) else Color(0xFF1E2E4A)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = if (isMuted) Color(0xFFEF4444) else CyanNeonPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isMuted) "Unmute" else "Mute Mic", color = if (isMuted) Color(0xFFEF4444) else TextPrimary, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { micController.checkBluetoothStatus() },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF1E2E4A)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = CyanNeonPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh BT", color = TextPrimary, fontSize = 12.sp)
            }
        }

        // Main Stream Button
        Button(
            onClick = {
                if (!micController.hasRecordPermission()) {
                    Toast.makeText(context, "Please grant microphone permission in device settings", Toast.LENGTH_SHORT).show()
                } else {
                    val nowPlaying = micController.toggleStreaming()
                    Toast.makeText(context, if (nowPlaying) "Live Mic started" else "Live Mic stopped", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlaying) Color(0xFFEF4444) else CyanNeonPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Close else Icons.Rounded.Mic,
                contentDescription = null,
                tint = if (isPlaying) Color.White else CyanOnPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isPlaying) "Stop Live Mic Loopback" else "Start Live Mic Loopback",
                color = if (isPlaying) Color.White else CyanOnPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 7. Notifications Config
@Composable
private fun NotificationsConfigContent(
    notificationRepository: NotificationHistoryRepository,
    installedAppsRepository: InstalledAppsRepository
) {
    val notifications by notificationRepository.notifications.collectAsStateWithLifecycle()
    val retention by notificationRepository.selectedRetention.collectAsStateWithLifecycle()
    val isCaptureEnabled by notificationRepository.isCaptureEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasPermission = remember(context) {
        NotificationPermissionHelper.isNotificationAccessGranted(context)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Vault Summary & Action Header
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF10192A),
            border = BorderStroke(1.dp, Color(0xFF1E2D48)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Captured Notifications: ${notifications.size}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = if (isCaptureEnabled) "Sentinel active • Preserving locally" else "Sentinel paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCaptureEnabled) CyanNeonPrimary else TextSecondary
                        )
                    }

                    if (notifications.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { notificationRepository.clearAll() },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Text("Clear All", color = Color(0xFFEF4444), fontSize = 11.5.sp)
                        }
                    }
                }

                // Permission Warning or Status
                if (!hasPermission) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF261A10),
                        border = BorderStroke(1.dp, Color(0xFF8C531B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Notification Listener access required", fontSize = 11.5.sp, color = Color(0xFFFFB020), modifier = Modifier.weight(1f))
                            Button(
                                onClick = { NotificationPermissionHelper.openNotificationListenerSettings(context) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Grant", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Test Notification & Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    notificationRepository.addNotification(
                        packageName = context.packageName,
                        appName = "HIRIS Controller",
                        title = "HIRIS Neural Link Active",
                        message = "System operational at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}. Cybernetic telemetry connected."
                    )
                    Toast.makeText(context, "Test notification preserved in vault!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = CyanNeonPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Test Item", color = CyanNeonPrimary, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { NotificationPermissionHelper.openNotificationListenerSettings(context) },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF1E2E4A)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Access Settings", color = TextSecondary, fontSize = 12.sp)
            }
        }

        // Retention Selector
        Text("Retention Policy", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RetentionPeriod.values().forEach { period ->
                val selected = retention == period
                Surface(
                    onClick = { notificationRepository.setRetentionPeriod(period) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) Color(0xFF162B46) else Color(0xFF101828),
                    border = BorderStroke(1.dp, if (selected) CyanNeonPrimary else Color(0xFF1C2A44)),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(period.code, fontSize = 12.sp, color = if (selected) CyanNeonPrimary else TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Recent Notifications Preview
        if (notifications.isNotEmpty()) {
            Text("Recent Activity (${notifications.size})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            notifications.take(5).forEach { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F1626),
                    border = BorderStroke(1.dp, Color(0xFF19253B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawableAppIcon(
                            packageName = item.packageName,
                            installedAppsRepository = installedAppsRepository,
                            size = 32.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.appName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeonPrimary
                                )
                                Text(
                                    text = android.text.format.DateUtils.getRelativeTimeSpanString(item.timestamp).toString(),
                                    fontSize = 10.sp,
                                    color = TextTertiary
                                )
                            }
                            Text(
                                text = item.title,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (item.message.isNotEmpty()) {
                                Text(
                                    text = item.message,
                                    fontSize = 11.5.sp,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { notificationRepository.deleteNotification(item.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color(0xFF8C9FB5), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1728))
            .border(BorderStroke(1.dp, Color(0xFF1B273E)), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.5.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CyanNeonPrimary,
                checkedBorderColor = CyanNeonPrimary,
                uncheckedThumbColor = Color(0xFF8899AC),
                uncheckedTrackColor = Color(0xFF182335)
            )
        )
    }
}
