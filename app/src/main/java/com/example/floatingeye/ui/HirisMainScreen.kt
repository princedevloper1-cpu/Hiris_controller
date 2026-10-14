package com.example.floatingeye.ui

import android.app.Activity
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.floatingeye.audio.BluetoothMicController
import com.example.floatingeye.battery.BatterySettingsRepository
import com.example.floatingeye.battery.FloatingEyeBatteryController
import com.example.floatingeye.controller.FloatingEyeLightController
import com.example.floatingeye.extensions.repository.ExtensionRepository
import com.example.floatingeye.firewall.repository.FirewallRepository
import com.example.floatingeye.gestures.GestureRepository
import com.example.floatingeye.model.FloatingEyeConfig
import com.example.floatingeye.notifications.NotificationHistoryRepository
import com.example.floatingeye.overlay.FloatingEyeOverlayService
import com.example.floatingeye.overlay.FloatingEyePositionManager
import com.example.floatingeye.overlay.OverlayPermissionManager
import com.example.floatingeye.shortcuts.AppShortcutRepository
import com.example.floatingeye.shortcuts.InstalledAppsRepository
import com.example.floatingeye.ui.extensions.ExtensionsMarketScreen
import com.example.floatingeye.ui.navigation.ExecutiveNavigationDrawerContent
import com.example.floatingeye.ui.navigation.ExecutiveTopAppBar
import com.example.floatingeye.ui.navigation.HirisBottomNavBar
import com.example.floatingeye.ui.navigation.HirisNavScreen
import com.example.floatingeye.ui.navigation.SlidingQuickAccessSheet
import com.example.floatingeye.ui.screens.EyeStudioScreen
import com.example.floatingeye.ui.screens.HomeScreen
import com.example.floatingeye.ui.screens.ProfileScreen
import kotlinx.coroutines.launch

@Composable
fun HirisMainScreen(
    positionManager: FloatingEyePositionManager,
    config: FloatingEyeConfig,
    initialExtensionId: String? = null,
    firewallVpnRequestId: Int = 0
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Persistent Repositories
    val batterySettingsRepository = remember { BatterySettingsRepository(context) }
    val notificationRepository = remember { NotificationHistoryRepository.getInstance(context) }
    val extensionRepository = remember {
        ExtensionRepository(context, batterySettingsRepository, notificationRepository)
    }
    val shortcutRepository = remember { AppShortcutRepository(context) }
    val installedAppsRepository = remember { InstalledAppsRepository(context) }
    val gestureRepository = remember { GestureRepository(context) }
    val lightController = remember { FloatingEyeLightController(config.bloomEnabled, config.bloomStrength) }
    val batteryController = remember {
        FloatingEyeBatteryController(context.applicationContext, lightController, batterySettingsRepository)
    }
    val firewallRepository = remember { FirewallRepository.getInstance(context) }
    val micController = remember { BluetoothMicController.getInstance(context) }
    val taskRepository = remember { com.example.floatingeye.tasks.repository.TaskRepository.getInstance(context) }

    DisposableEffect(batteryController) {
        batteryController.startListening()
        onDispose {
            batteryController.stopListening()
        }
    }

    // System States
    val isOverlayRunning by FloatingEyeOverlayService.isServiceRunning.collectAsStateWithLifecycle()
    val isFirewallActive by firewallRepository.isFirewallActive.collectAsStateWithLifecycle()
    val blockedRules by firewallRepository.rules.collectAsStateWithLifecycle()
    val blockedAppsCount = remember(blockedRules) {
        blockedRules.values.count { it.isAnyBlocked }
    }
    val batteryInfo by batteryController.batteryInfo.collectAsStateWithLifecycle()
    val extensions by extensionRepository.extensions.collectAsStateWithLifecycle()
    val activeModulesCount = remember(extensions) {
        extensions.count { it.isActive }
    }

    // Navigation and Sheet States
    var currentScreen by remember { mutableStateOf(HirisNavScreen.EXTENSIONS) }
    var activeConfigExtensionId by remember { mutableStateOf<String?>(initialExtensionId) }
    var showQuickAccessSheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialExtensionId) {
        if (initialExtensionId != null) {
            currentScreen = HirisNavScreen.EXTENSIONS
            activeConfigExtensionId = initialExtensionId
        }
    }

    // VPN launcher for firewall activation
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            extensionRepository.toggleActive(ExtensionRepository.ID_FIREWALL, true)
            Toast.makeText(context, "Firewall Shield Activated • Traffic Filter Active", Toast.LENGTH_SHORT).show()
        } else {
            extensionRepository.toggleActive(ExtensionRepository.ID_FIREWALL, false)
            Toast.makeText(context, "VPN permission required for Network Firewall", Toast.LENGTH_LONG).show()
        }
    }

    val requestFirewallEnable = {
        if (!extensionRepository.canActivate(ExtensionRepository.ID_FIREWALL)) {
            Toast.makeText(
                context,
                "Floating Eye ka montre 8 app ak extension sèlman. Retire youn anvan ou aktive Firewall.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else {
                extensionRepository.toggleActive(ExtensionRepository.ID_FIREWALL, true)
            }
        }
        Unit
    }

    LaunchedEffect(firewallVpnRequestId) {
        if (firewallVpnRequestId > 0 && !firewallRepository.isFirewallActive.value) {
            requestFirewallEnable()
        }
    }

    val toggleOverlay = {
        if (isOverlayRunning) {
            FloatingEyeOverlayService.stop(context)
            Toast.makeText(context, "Floating Eye Stopped", Toast.LENGTH_SHORT).show()
        } else {
            if (!OverlayPermissionManager.canDrawOverlays(context)) {
                OverlayPermissionManager.openOverlayPermissionSettings(context)
            } else {
                FloatingEyeOverlayService.start(context)
                Toast.makeText(context, "Floating Eye Launched (+)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val toggleFirewall = {
        if (isFirewallActive) {
            extensionRepository.toggleActive(ExtensionRepository.ID_FIREWALL, false)
            Toast.makeText(context, "Firewall Shield Deactivated", Toast.LENGTH_SHORT).show()
        } else {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else {
                extensionRepository.toggleActive(ExtensionRepository.ID_FIREWALL, true)
                Toast.makeText(context, "Firewall Shield Activated • Traffic Filter Active", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // BackHandler Priority: Close drawer -> Close quick sheet -> Return to Modules screen
    BackHandler(enabled = drawerState.isOpen || showQuickAccessSheet || currentScreen != HirisNavScreen.EXTENSIONS) {
        when {
            drawerState.isOpen -> {
                coroutineScope.launch { drawerState.close() }
            }
            showQuickAccessSheet -> {
                showQuickAccessSheet = false
            }
            currentScreen != HirisNavScreen.EXTENSIONS -> {
                currentScreen = HirisNavScreen.EXTENSIONS
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ExecutiveNavigationDrawerContent(
                currentScreen = currentScreen,
                isOverlayRunning = isOverlayRunning,
                isFirewallActive = isFirewallActive,
                blockedAppsCount = blockedAppsCount,
                batteryPercentage = batteryInfo.percentage,
                activeModulesCount = activeModulesCount,
                onNavigate = { screen ->
                    currentScreen = screen
                },
                onToggleOverlay = toggleOverlay,
                onToggleFirewall = toggleFirewall,
                onOpenModuleConfig = { moduleKey ->
                    val extId = when (moduleKey) {
                        "firewall" -> ExtensionRepository.ID_FIREWALL
                        "tasks_reminders" -> ExtensionRepository.ID_TASKS
                        "power_drain_monitor" -> ExtensionRepository.ID_DRAIN_MONITOR
                        "notification_sentinel" -> ExtensionRepository.ID_NOTIFICATIONS
                        "shortcuts" -> ExtensionRepository.ID_SHORTCUTS
                        "bluetooth_mic" -> ExtensionRepository.ID_BLUETOOTH_MIC
                        else -> null
                    }
                    if (extId != null) {
                        currentScreen = HirisNavScreen.EXTENSIONS
                        activeConfigExtensionId = extId
                    }
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080C14)),
            containerColor = Color(0xFF080C14),
            topBar = {
                ExecutiveTopAppBar(
                    currentScreen = currentScreen,
                    isOverlayRunning = isOverlayRunning,
                    isFirewallActive = isFirewallActive,
                    blockedAppsCount = blockedAppsCount,
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    onToggleOverlay = toggleOverlay,
                    onOpenQuickPanel = {
                        showQuickAccessSheet = true
                    }
                )
            },
            bottomBar = {
                HirisBottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    )
            ) {
                Crossfade(
                    targetState = currentScreen,
                    animationSpec = tween(220),
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        HirisNavScreen.HOME -> {
                            HomeScreen(
                                positionManager = positionManager,
                                config = config,
                                extensionRepository = extensionRepository,
                                batteryController = batteryController,
                                shortcutRepository = shortcutRepository,
                                onNavigateToExtensions = {
                                    currentScreen = HirisNavScreen.EXTENSIONS
                                }
                            )
                        }
                        HirisNavScreen.EXTENSIONS -> {
                            ExtensionsMarketScreen(
                                extensionRepository = extensionRepository,
                                shortcutRepository = shortcutRepository,
                                installedAppsRepository = installedAppsRepository,
                                gestureRepository = gestureRepository,
                                batterySettingsRepository = batterySettingsRepository,
                                batteryController = batteryController,
                                micController = micController,
                                notificationRepository = notificationRepository,
                                positionManager = positionManager,
                                config = config,
                                taskRepository = taskRepository,
                                onRequestFirewallEnable = requestFirewallEnable,
                                initialConfigExtensionId = activeConfigExtensionId
                            )
                        }
                        HirisNavScreen.EYE -> {
                            EyeStudioScreen(
                                config = config,
                                positionManager = positionManager
                            )
                        }
                        HirisNavScreen.PROFILE -> {
                            ProfileScreen(
                                micController = micController
                            )
                        }
                    }
                }
            }
        }
    }

    // Sliding Quick Access Panel (Bottom Sheet)
    if (showQuickAccessSheet) {
        SlidingQuickAccessSheet(
            firewallRepository = firewallRepository,
            isOverlayRunning = isOverlayRunning,
            isFirewallActive = isFirewallActive,
            onToggleOverlay = toggleOverlay,
            onToggleFirewall = toggleFirewall,
            onOpenFullFirewall = {
                currentScreen = HirisNavScreen.EXTENSIONS
                activeConfigExtensionId = ExtensionRepository.ID_FIREWALL
            },
            onDismiss = {
                showQuickAccessSheet = false
            }
        )
    }
}
