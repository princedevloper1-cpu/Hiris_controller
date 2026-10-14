package com.example.floatingeye.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.floatingeye.battery.BatterySettingsRepository
import com.example.floatingeye.battery.FloatingEyeBatteryController
import com.example.floatingeye.extensions.repository.ExtensionRepository
import com.example.floatingeye.notifications.NotificationHistoryRepository
import com.example.floatingeye.overlay.FloatingEyePositionManager
import com.example.floatingeye.tasks.repository.TaskRepository
import com.example.floatingeye.tasks.model.TaskReminderDraft
import com.example.floatingeye.gestures.GestureRepository
import com.example.floatingeye.gestures.GestureSetupFlowDialog
import com.example.floatingeye.model.FloatingEyeConfig
import com.example.floatingeye.shortcuts.InstalledAppsRepository
import com.example.floatingeye.ui.extensions.GesturesConfigContent
import com.example.floatingeye.ui.extensions.BatteryReactionsConfigContent
import com.example.floatingeye.ui.CyberPowerDrainCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.floatingeye.ui.CyberBluetoothMicCard
import com.example.floatingeye.ui.CyberNotificationHistoryCard
import com.example.floatingeye.ui.tasks.CyberTaskManagerCard
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * High-End Floating In-Screen Window for any active extension clicked from the Floating Eye.
 * Allows the user to use Tasks, Notifications, and other extensions directly in place
 * over any app or home screen WITHOUT leaving their current context or launching MainActivity.
 */
@Composable
fun FloatingExtensionWindow(
    activeExtensionId: String?,
    onClose: () -> Unit,
    onMinimizedChanged: (Boolean) -> Unit,
    onDrag: (Float, Float) -> Unit,
    onOpenFullApp: (String) -> Unit,
    onFloatingCaptureStart: (mode: String, areaMode: String) -> Unit,
    onSwitchExtension: (String) -> Unit,
    taskRepository: TaskRepository,
    notificationRepository: NotificationHistoryRepository,
    batterySettingsRepository: BatterySettingsRepository,
    batteryController: FloatingEyeBatteryController,
    micController: com.example.floatingeye.audio.BluetoothMicController,
    positionManager: FloatingEyePositionManager,
    gestureRepository: GestureRepository,
    installedAppsRepository: InstalledAppsRepository,
    config: FloatingEyeConfig,
    onFirewallActiveChanged: (Boolean) -> Unit,
    onRequestFirewallEnable: () -> Unit,
    firewallRepository: com.example.floatingeye.firewall.repository.FirewallRepository? = null,
    modifier: Modifier = Modifier
) {
    if (activeExtensionId == null) return
    var showGestureSetup by remember(activeExtensionId) { mutableStateOf(false) }
    var isMinimized by remember(activeExtensionId) { mutableStateOf(false) }

    val extensionTitle = when (activeExtensionId) {
        ExtensionRepository.ID_FIREWALL -> "Firewall & App Blocker"
        ExtensionRepository.ID_TASKS -> "Tasks & Reminders"
        ExtensionRepository.ID_CAPTURE -> "Hiris Capture"
        ExtensionRepository.ID_NOTIFICATIONS -> "Notification Sentinel"
        ExtensionRepository.ID_BLUETOOTH_MIC -> "Bluetooth Mic Studio"
        ExtensionRepository.ID_GESTURES -> "Gesture Settings"
        ExtensionRepository.ID_BATTERY -> "Battery Reactions"
        ExtensionRepository.ID_DRAIN_MONITOR -> "Power Drain Monitor"
        else -> "HIRIS Extension"
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isMinimized) {
            Surface(
                onClick = {
                    isMinimized = false
                    onMinimizedChanged(false)
                },
                shape = CircleShape,
                color = Color(0xFF0C121D),
                border = BorderStroke(2.dp, CyanNeonPrimary),
                shadowElevation = 12.dp,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(extensionTitle.take(1), color = CyanNeonPrimary, fontWeight = FontWeight.Bold)
                }
            }
            return
        }

        // Floating Modal Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0C121D),
            border = BorderStroke(1.2.dp, CyanNeonPrimary.copy(alpha = 0.5f)),
            shadowElevation = 18.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top Header Bar: Drag Handle, Title, and Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(activeExtensionId) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    onDrag(drag.x, drag.y)
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(CyanNeonPrimary)
                        )
                        Text(
                            text = extensionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isMinimized = true
                                onMinimizedChanged(true)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Minimize floating window",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Open full app button
                        IconButton(
                            onClick = { onOpenFullApp(activeExtensionId) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Maximize in app: $extensionTitle",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close floating window button
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Floating Window",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content Area for the clicked extension
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    when (activeExtensionId) {
                        ExtensionRepository.ID_CAPTURE -> {
                            com.example.floatingeye.capture.HirisCaptureCard(
                                modifier = Modifier.fillMaxWidth(),
                                onFloatingCaptureStart = onFloatingCaptureStart
                            )
                        }
                        ExtensionRepository.ID_FIREWALL -> {
                            val effectiveFirewall = remember(context, firewallRepository) {
                                firewallRepository ?: com.example.floatingeye.firewall.repository.FirewallRepository.getInstance(context)
                            }
                            com.example.floatingeye.ui.firewall.CyberFirewallCard(
                                repository = effectiveFirewall,
                                installedAppsRepository = installedAppsRepository,
                                onFirewallActiveChanged = onFirewallActiveChanged,
                                onRequestFirewallEnable = onRequestFirewallEnable,
                                initiallyExpanded = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ExtensionRepository.ID_TASKS -> {
                            CyberTaskManagerCard(
                                repository = taskRepository,
                                initiallyExpanded = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ExtensionRepository.ID_NOTIFICATIONS -> {
                            CyberNotificationHistoryCard(
                                repository = notificationRepository,
                                initiallyExpanded = true,
                                modifier = Modifier.fillMaxWidth(),
                                onCreateReminder = { notification ->
                                    taskRepository.setReminderDraft(
                                        TaskReminderDraft(
                                            title = notification.senderName?.takeIf { it.isNotBlank() }
                                                ?: notification.title.ifBlank { notification.appName },
                                            description = notification.message.ifBlank { notification.title }
                                        )
                                    )
                                    onSwitchExtension(ExtensionRepository.ID_TASKS)
                                }
                            )
                        }
                        ExtensionRepository.ID_BLUETOOTH_MIC -> {
                            CyberBluetoothMicCard(
                                micController = micController,
                                initiallyExpanded = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ExtensionRepository.ID_BATTERY -> {
                            BatteryReactionsConfigContent(
                                batterySettingsRepository = batterySettingsRepository,
                                batteryController = batteryController,
                                onSimulateBatteryState = { state ->
                                    val simulated = when (state) {
                                        "charging" -> com.example.floatingeye.battery.BatteryInfo(55, true, false)
                                        "full" -> com.example.floatingeye.battery.BatteryInfo(100, true, true)
                                        "low" -> com.example.floatingeye.battery.BatteryInfo(20, false, false)
                                        "critical" -> com.example.floatingeye.battery.BatteryInfo(10, false, false)
                                        else -> null
                                    }
                                    batteryController.setSimulationOverride(simulated)
                                }
                            )
                        }
                        ExtensionRepository.ID_DRAIN_MONITOR -> {
                            val isEnabled by batterySettingsRepository.drainMonitorEnabled.collectAsStateWithLifecycle()
                            val telemetry by batteryController.drainMonitor.telemetry.collectAsStateWithLifecycle()
                            CyberPowerDrainCard(
                                isEnabled = isEnabled,
                                telemetry = telemetry,
                                settingsRepository = batterySettingsRepository,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ExtensionRepository.ID_GESTURES -> {
                            GesturesConfigContent(
                                gestureRepository = gestureRepository,
                                onAddGestureClick = { showGestureSetup = true }
                            )
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Extension active: $extensionTitle",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Use the maximize button to open these settings in HIRIS.",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showGestureSetup) {
        GestureSetupFlowDialog(
            installedAppsRepository = installedAppsRepository,
            gestureRepository = gestureRepository,
            config = config,
            onDismiss = { showGestureSetup = false },
            onCompleted = { showGestureSetup = false }
        )
    }
}
