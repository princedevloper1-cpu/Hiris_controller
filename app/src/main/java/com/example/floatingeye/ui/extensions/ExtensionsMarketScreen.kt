package com.example.floatingeye.ui.extensions

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.floatingeye.notifications.NotificationPermissionHelper
import com.example.floatingeye.tasks.repository.TaskRepository
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.audio.BluetoothMicController
import com.example.floatingeye.battery.BatteryInfo
import com.example.floatingeye.battery.BatterySettingsRepository
import com.example.floatingeye.battery.FloatingEyeBatteryController
import com.example.floatingeye.extensions.model.ExtensionCategory
import com.example.floatingeye.extensions.model.HirisExtension
import com.example.floatingeye.extensions.repository.ExtensionRepository
import com.example.floatingeye.gestures.GestureRepository
import com.example.floatingeye.gestures.GestureSetupFlowDialog
import com.example.floatingeye.model.FloatingEyeConfig
import com.example.floatingeye.notifications.NotificationHistoryRepository
import com.example.floatingeye.tasks.model.TaskReminderDraft
import com.example.floatingeye.overlay.FloatingEyePositionManager
import com.example.floatingeye.shortcuts.AppShortcut
import com.example.floatingeye.shortcuts.AppShortcutPicker
import com.example.floatingeye.shortcuts.AppShortcutRepository
import com.example.floatingeye.shortcuts.InstalledAppsRepository
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

enum class ModuleFilter {
    ALL,
    ACTIVE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsMarketScreen(
    extensionRepository: ExtensionRepository,
    shortcutRepository: AppShortcutRepository,
    installedAppsRepository: InstalledAppsRepository,
    gestureRepository: GestureRepository,
    batterySettingsRepository: BatterySettingsRepository,
    batteryController: FloatingEyeBatteryController,
    micController: BluetoothMicController,
    notificationRepository: NotificationHistoryRepository,
    positionManager: FloatingEyePositionManager,
    config: FloatingEyeConfig,
    taskRepository: TaskRepository? = null,
    onRequestFirewallEnable: () -> Unit,
    initialConfigExtensionId: String? = null,
    modifier: Modifier = Modifier
) {
    val extensions by extensionRepository.extensions.collectAsStateWithLifecycle()
    val shortcuts by shortcutRepository.shortcuts.collectAsStateWithLifecycle()
    val isPlaying by micController.isMicPlaying.collectAsStateWithLifecycle()
    val notificationsList by notificationRepository.notifications.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val effectiveTaskRepo = taskRepository ?: remember { TaskRepository.getInstance(context) }

    var selectedFilter by remember { mutableStateOf(ModuleFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ExtensionCategory.ALL) }
    var showFilterChips by remember { mutableStateOf(false) }
    var permissionCheckVersion by remember { mutableIntStateOf(0) }

    // Configuration Sheet State
    var configuringExtension by remember {
        mutableStateOf<HirisExtension?>(
            initialConfigExtensionId?.let { id -> extensions.firstOrNull { it.id == id } }
        )
    }

    // This is a one-time destination passed when opening the market from an
    // extension shortcut. Do not reapply it when the extension list changes
    // (for example, after activating a different extension), or its config
    // sheet will unexpectedly reopen.
    LaunchedEffect(initialConfigExtensionId) {
        if (initialConfigExtensionId != null) {
            val found = extensions.firstOrNull { it.id == initialConfigExtensionId }
            if (found != null) {
                configuringExtension = found
            }
        }
    }
    var showAppPicker by remember { mutableStateOf(false) }
    var showGestureSetup by remember { mutableStateOf(false) }

    fun startMicStreaming() {
        if (micController.startStreaming()) {
            extensionRepository.toggleActive(ExtensionRepository.ID_BLUETOOTH_MIC, true)
            Toast.makeText(context, "Bluetooth Mic activated (Streaming live audio)", Toast.LENGTH_SHORT).show()
        } else {
            extensionRepository.toggleActive(ExtensionRepository.ID_BLUETOOTH_MIC, false)
            Toast.makeText(context, micController.errorMessage.value ?: "Could not start Bluetooth Mic", Toast.LENGTH_LONG).show()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startMicStreaming()
        } else {
            extensionRepository.toggleActive(ExtensionRepository.ID_BLUETOOTH_MIC, false)
            Toast.makeText(context, "Microphone permission required to activate", Toast.LENGTH_SHORT).show()
        }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!micController.hasRecordPermission()) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                startMicStreaming()
            }
        } else {
            extensionRepository.toggleActive(ExtensionRepository.ID_BLUETOOTH_MIC, false)
            Toast.makeText(context, "Bluetooth access is required for the selected audio route", Toast.LENGTH_LONG).show()
        }
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            extensionRepository.toggleActive(ExtensionRepository.ID_FIREWALL, true)
            Toast.makeText(context, "App Firewall & Data Blocker activated • Shield ON", Toast.LENGTH_SHORT).show()
        } else {
            extensionRepository.toggleActive(ExtensionRepository.ID_FIREWALL, false)
            Toast.makeText(context, "VPN authorization is required to activate Firewall", Toast.LENGTH_LONG).show()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, extensions) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionCheckVersion++
                val notifExt = extensions.firstOrNull { it.id == ExtensionRepository.ID_NOTIFICATIONS }
                if (notifExt?.isActive == true) {
                    if (NotificationPermissionHelper.isNotificationAccessGranted(context)) {
                        notificationRepository.setCaptureEnabled(true)
                    } else {
                        extensionRepository.toggleActive(ExtensionRepository.ID_NOTIFICATIONS, false)
                        Toast.makeText(context, "Notification access was revoked; Sentinel is now off", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val filteredExtensions = remember(extensions, selectedFilter, selectedCategory, searchQuery) {
        extensions.filter { ext ->
            val matchesFilter = when (selectedFilter) {
                ModuleFilter.ALL -> true
                ModuleFilter.ACTIVE -> ext.isActive
            }
            val matchesCategory = (selectedCategory == ExtensionCategory.ALL || ext.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() ||
                ext.name.contains(searchQuery, ignoreCase = true) ||
                ext.description.contains(searchQuery, ignoreCase = true) ||
                ext.categoryTags.any { it.contains(searchQuery, ignoreCase = true) }

            matchesFilter && matchesCategory && matchesSearch
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080C14))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Row
            item {
                HeaderRow(
                    isSearchOpen = isSearchOpen,
                    onToggleSearch = {
                        isSearchOpen = !isSearchOpen
                        if (!isSearchOpen) searchQuery = ""
                    },
                    showFilterChips = showFilterChips,
                    onToggleFilters = { showFilterChips = !showFilterChips }
                )
            }

            // 1b. Search Input (Animated Visibility)
            item {
                AnimatedVisibility(
                    visible = isSearchOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .testTag("extensions_search_input"),
                        placeholder = { Text("Search extensions, tags...", color = TextTertiary, fontSize = 13.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = CyanNeonPrimary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = TextTertiary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeonPrimary,
                            unfocusedBorderColor = Color(0xFF1E2B42),
                            focusedContainerColor = Color(0xFF0E1524),
                            unfocusedContainerColor = Color(0xFF0E1524),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }

            // 1c. Category Filter Chips (Animated Visibility)
            item {
                AnimatedVisibility(
                    visible = showFilterChips,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        items(ExtensionCategory.values()) { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = {
                                    Text(
                                        text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF15263F),
                                    selectedLabelColor = CyanNeonPrimary,
                                    containerColor = Color(0xFF0E1524),
                                    labelColor = TextSecondary
                                ),
                                border = BorderStroke(1.dp, if (isSelected) CyanNeonPrimary else Color(0xFF1C283E))
                            )
                        }
                    }
                }
            }

            // 2. Module scope selector
            item {
                ModuleFilterSelector(
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it },
                    moduleCount = extensions.size,
                    activeCount = extensions.count { it.isActive }
                )
            }

            // 3. Module status summary
            item {
                ModuleSummaryCard(
                    moduleCount = extensions.size,
                    activeCount = extensions.count { it.isActive }
                )
            }

            // 3b. Active Extension Live Status Banner (Bluetooth Mic & Notifications)
            val micActive = extensions.firstOrNull { it.id == ExtensionRepository.ID_BLUETOOTH_MIC }?.isActive == true
            val notifActive = extensions.firstOrNull { it.id == ExtensionRepository.ID_NOTIFICATIONS }?.isActive == true

            if (micActive || notifActive) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (micActive) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0F1E2E),
                                border = BorderStroke(1.dp, if (isPlaying) Color(0xFF38E07B) else CyanNeonPrimary.copy(alpha = 0.5f)),
                                onClick = {
                                    configuringExtension = extensions.firstOrNull { it.id == ExtensionRepository.ID_BLUETOOTH_MIC }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (isPlaying) Color(0xFF38E07B) else CyanNeonPrimary)
                                        )
                                        Column {
                                            Text(
                                                text = if (isPlaying) "Bluetooth Mic Active (Live Streaming)" else "Bluetooth Mic Enabled (Ready)",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPlaying) Color(0xFF38E07B) else CyanNeonPrimary
                                            )
                                            Text(
                                                text = "Tap to open audio gain & output settings",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = "Settings",
                                        tint = CyanNeonPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (notifActive) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF14192B),
                                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                                onClick = {
                                    configuringExtension = extensions.firstOrNull { it.id == ExtensionRepository.ID_NOTIFICATIONS }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF8B5CF6))
                                        )
                                        Column {
                                            Text(
                                                text = "Notification Sentinel Active (${notificationsList.size} captured)",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC4B5FD)
                                            )
                                            Text(
                                                text = "Tap to inspect captured notifications & media",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = "Inspect",
                                        tint = Color(0xFFC4B5FD),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Extensions Vertical List
            if (filteredExtensions.isEmpty()) {
                item {
                    EmptyExtensionsView(
                        filter = selectedFilter,
                        searchQuery = searchQuery,
                        onClearSearch = { searchQuery = "" }
                    )
                }
            } else {
                items(
                    items = filteredExtensions,
                    key = { it.id }
                ) { extension ->
                    val needsAccess = remember(extension.id, permissionCheckVersion) {
                        when (extension.id) {
                            ExtensionRepository.ID_BLUETOOTH_MIC ->
                                !micController.hasRecordPermission() ||
                                    (micController.selectedOutputRoute.value == com.example.floatingeye.audio.MicOutputRoute.BLUETOOTH &&
                                        !micController.hasBluetoothConnectPermission())
                            ExtensionRepository.ID_NOTIFICATIONS ->
                                !NotificationPermissionHelper.isNotificationAccessGranted(context)
                            ExtensionRepository.ID_FIREWALL -> VpnService.prepare(context) != null
                            else -> false
                        }
                    }
                    ExtensionCard(
                        extension = extension,
                        needsAccess = needsAccess,
                        onToggleActive = { active ->
                            when (extension.id) {
                                ExtensionRepository.ID_BLUETOOTH_MIC -> {
                                    if (active) {
                                        if (
                                            micController.selectedOutputRoute.value == com.example.floatingeye.audio.MicOutputRoute.BLUETOOTH &&
                                            !micController.hasBluetoothConnectPermission()
                                        ) {
                                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                        } else if (!micController.hasRecordPermission()) {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            startMicStreaming()
                                        }
                                    } else {
                                        micController.stopStreaming()
                                        extensionRepository.toggleActive(extension.id, false)
                                        Toast.makeText(context, "Bluetooth Mic deactivated", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                ExtensionRepository.ID_NOTIFICATIONS -> {
                                    if (active) {
                                        if (!NotificationPermissionHelper.isNotificationAccessGranted(context)) {
                                            Toast.makeText(context, "Please allow Notification Access to capture system notifications", Toast.LENGTH_LONG).show()
                                            NotificationPermissionHelper.openNotificationListenerSettings(context)
                                        } else {
                                            extensionRepository.toggleActive(extension.id, true)
                                            Toast.makeText(context, "Notification Sentinel activated (Capturing notifications)", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        extensionRepository.toggleActive(extension.id, false)
                                        Toast.makeText(context, "Notification Sentinel deactivated", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                ExtensionRepository.ID_BATTERY -> {
                                    batterySettingsRepository.setReactionsEnabled(active)
                                    extensionRepository.toggleActive(extension.id, active)
                                    Toast.makeText(context, if (active) "Battery Reactions activated • Arc Reactor ON" else "Battery Reactions deactivated", Toast.LENGTH_SHORT).show()
                                }
                                ExtensionRepository.ID_DRAIN_MONITOR -> {
                                    batterySettingsRepository.setDrainMonitorEnabled(active)
                                    extensionRepository.toggleActive(extension.id, active)
                                    Toast.makeText(context, if (active) "Power Drain Monitor activated" else "Power Drain Monitor deactivated", Toast.LENGTH_SHORT).show()
                                }
                                ExtensionRepository.ID_SHORTCUTS -> {
                                    extensionRepository.toggleActive(extension.id, active)
                                    Toast.makeText(context, if (active) "Radial Shortcuts activated" else "Radial Shortcuts deactivated", Toast.LENGTH_SHORT).show()
                                }
                                ExtensionRepository.ID_GESTURES -> {
                                    extensionRepository.toggleActive(extension.id, active)
                                    Toast.makeText(context, if (active) "Gesture Launch activated" else "Gesture Launch deactivated", Toast.LENGTH_SHORT).show()
                                }
                                ExtensionRepository.ID_TASKS -> {
                                    extensionRepository.toggleActive(extension.id, active)
                                    Toast.makeText(context, if (active) "Tasks & Reminders activated • Mounted to Floating Eye!" else "Tasks deactivated", Toast.LENGTH_SHORT).show()
                                }
                                ExtensionRepository.ID_FIREWALL -> {
                                    if (active) {
                                        val prepareIntent = android.net.VpnService.prepare(context)
                                        if (prepareIntent != null) {
                                            vpnPermissionLauncher.launch(prepareIntent)
                                        } else {
                                            extensionRepository.toggleActive(extension.id, true)
                                            Toast.makeText(context, "App Firewall & Data Blocker activated • Shield ON", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        extensionRepository.toggleActive(extension.id, false)
                                        Toast.makeText(context, "Firewall deactivated", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                else -> {
                                    extensionRepository.toggleActive(extension.id, active)
                                    Toast.makeText(context, "${extension.name} ${if (active) "activated" else "deactivated"}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onOpenSettings = {
                            configuringExtension = extension
                        }
                    )
                }
            }

            // Bottom spacing for navigation bar
            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }

    // Modal Configuration Sheet
    configuringExtension?.let { ext ->
        val currentExt = extensions.firstOrNull { it.id == ext.id } ?: ext
        ExtensionConfigSheet(
            extension = currentExt,
            onDismiss = { configuringExtension = null },
            shortcutRepository = shortcutRepository,
            installedAppsRepository = installedAppsRepository,
            gestureRepository = gestureRepository,
            batterySettingsRepository = batterySettingsRepository,
            batteryController = batteryController,
            micController = micController,
            notificationRepository = notificationRepository,
            positionManager = positionManager,
            taskRepository = effectiveTaskRepo,
            onReminderRequested = { notification ->
                effectiveTaskRepo.setReminderDraft(
                    TaskReminderDraft(
                        title = notification.senderName?.takeIf { it.isNotBlank() }
                            ?: notification.title.ifBlank { notification.appName },
                        description = notification.message.ifBlank { notification.title }
                    )
                )
                configuringExtension = extensions.firstOrNull { it.id == ExtensionRepository.ID_TASKS }
            },
            onFirewallActiveChanged = { active ->
                extensionRepository.toggleActive(ExtensionRepository.ID_FIREWALL, active)
            },
            onRequestFirewallEnable = onRequestFirewallEnable,
            onAddShortcutClick = { showAppPicker = true },
            onAddGestureClick = { showGestureSetup = true },
            onSimulateBatteryState = { simState ->
                val override = when (simState) {
                    "charging" -> BatteryInfo(percentage = 55, isCharging = true, isFull = false)
                    "full" -> BatteryInfo(percentage = 100, isCharging = true, isFull = true)
                    "low" -> BatteryInfo(percentage = 20, isCharging = false, isFull = false)
                    "critical" -> BatteryInfo(percentage = 10, isCharging = false, isFull = false)
                    else -> null
                }
                batteryController.setSimulationOverride(override)
            }
        )
    }

    // Sub-dialog: App Shortcut Picker
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

    // Sub-dialog: Gesture Setup Flow
    if (showGestureSetup) {
        GestureSetupFlowDialog(
            installedAppsRepository = installedAppsRepository,
            gestureRepository = gestureRepository,
            config = config,
            onDismiss = { showGestureSetup = false },
            onCompleted = {
                showGestureSetup = false
            }
        )
    }
}

// -------------------------------------------------------------
// Sub-components
// -------------------------------------------------------------

@Composable
private fun HeaderRow(
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit,
    showFilterChips: Boolean,
    onToggleFilters: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "HIRIS Controller",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Module Manager",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF7E90A6)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Search Button
            Surface(
                onClick = onToggleSearch,
                shape = CircleShape,
                color = if (isSearchOpen) Color(0xFF16273F) else Color(0xFF111724),
                border = BorderStroke(1.dp, if (isSearchOpen) CyanNeonPrimary else Color(0xFF1D283C)),
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_search_extensions")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = if (isSearchOpen) CyanNeonPrimary else Color(0xFF8C9FB5),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Tune / Filter Button
            Surface(
                onClick = onToggleFilters,
                shape = CircleShape,
                color = if (showFilterChips) Color(0xFF16273F) else Color(0xFF111724),
                border = BorderStroke(1.dp, if (showFilterChips) CyanNeonPrimary else Color(0xFF1D283C)),
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_filter_extensions")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Filter",
                        tint = if (showFilterChips) CyanNeonPrimary else Color(0xFF8C9FB5),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleFilterSelector(
    selectedFilter: ModuleFilter,
    onSelectFilter: (ModuleFilter) -> Unit,
    moduleCount: Int,
    activeCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("module_filter_selector"),
        shape = RoundedCornerShape(23.dp),
        color = Color(0xFF0F1420),
        border = BorderStroke(1.dp, Color(0xFF1B2435))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // All bundled modules
            TabPillButton(
                title = "All modules",
                count = moduleCount,
                isSelected = selectedFilter == ModuleFilter.ALL,
                onClick = { onSelectFilter(ModuleFilter.ALL) },
                modifier = Modifier.weight(1f)
            )

            // Enabled modules
            TabPillButton(
                title = "Active",
                count = activeCount,
                isSelected = selectedFilter == ModuleFilter.ACTIVE,
                onClick = { onSelectFilter(ModuleFilter.ACTIVE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabPillButton(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF00D2FF),
            Color(0xFF0091FF)
        )
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier
                        .background(pillGradient)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp), spotColor = CyanNeonPrimary)
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = if (isSelected) Color.White else Color(0xFF7E90A6)
        )
    }
}

@Composable
private fun ModuleSummaryCard(moduleCount: Int, activeCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("module_summary_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1525)
        ),
        border = BorderStroke(1.dp, Color(0xFF1B2B45))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HIRIS Modules",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.5.sp
                    ),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$moduleCount modules included · $activeCount active",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    ),
                    color = Color(0xFF8B9DB3)
                )
            }

        }
    }
}

@Composable
private fun EmptyExtensionsView(
    filter: ModuleFilter,
    searchQuery: String,
    onClearSearch: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0E1524),
        border = BorderStroke(1.dp, Color(0xFF1A263C)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when {
                    searchQuery.isNotEmpty() -> "No matching modules found"
                    filter == ModuleFilter.ACTIVE -> "No active modules"
                    else -> "No modules found"
                },
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "Try searching for another keyword or clear your query."
                } else {
                    "No modules match the current filter."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onClearSearch,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyanNeonPrimary)
                ) {
                    Text("Clear Search", color = CyanNeonPrimary, fontSize = 12.sp)
                }
            }
        }
    }
}
