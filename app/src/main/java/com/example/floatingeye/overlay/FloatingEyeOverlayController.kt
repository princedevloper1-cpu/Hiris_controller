package com.example.floatingeye.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Widgets
import com.example.floatingeye.extensions.model.ActiveExtensionItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.floatingeye.battery.BatteryColors
import com.example.floatingeye.controller.FloatingEyeController
import com.example.floatingeye.controller.FloatingEyeLightController
import com.example.floatingeye.gestures.GestureCaptureController
import com.example.floatingeye.gestures.GestureDrawingCanvas
import com.example.floatingeye.gestures.GestureRecognizer
import com.example.floatingeye.gestures.GestureRepository
import com.example.floatingeye.gestures.RecognitionResult
import com.example.floatingeye.model.FloatingEyeConfig
import com.example.floatingeye.shortcuts.AppLaunchManager
import com.example.floatingeye.shortcuts.AppShortcut
import com.example.floatingeye.shortcuts.AppShortcutController
import com.example.floatingeye.shortcuts.AppShortcutRepository
import com.example.floatingeye.shortcuts.InstalledAppsRepository
import com.example.floatingeye.shortcuts.RadialAppShortcutMenu
import com.example.floatingeye.ui.FloatingEyePlusWidget
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.FloatingEyeTheme
import com.example.ui.theme.SurfaceDark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FloatingEyeOverlayController(
    private val context: Context,
    private val positionManager: FloatingEyePositionManager
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    val config = FloatingEyeConfig.loadFromAssets(context)
    val lightController = FloatingEyeLightController(
        config.bloomEnabled,
        positionManager.getBloomStrength(config.bloomStrength)
    )
    val eyeController = FloatingEyeController(config, lightController)

    val shortcutRepository = AppShortcutRepository(context)
    val installedAppsRepository = InstalledAppsRepository(context)
    val launchManager = AppLaunchManager(context, shortcutRepository)
    val shortcutController = AppShortcutController(shortcutRepository, launchManager)

    val gestureRepository = GestureRepository(context)
    val gestureCaptureController = GestureCaptureController()

    val batteryController = com.example.floatingeye.battery.FloatingEyeBatteryController(context, lightController)
    val micController = com.example.floatingeye.audio.BluetoothMicController.getInstance(context)
    val extensionRepository = com.example.floatingeye.extensions.repository.ExtensionRepository(context, batteryController.settingsRepository)

    private var overlayComposeView: ComposeView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private val _isShowing = MutableStateFlow(false)
    val isShowing: StateFlow<Boolean> = _isShowing.asStateFlow()

    private val _isEyeVisible = MutableStateFlow(true)
    val isEyeVisible: StateFlow<Boolean> = _isEyeVisible.asStateFlow()

    private val _isGestureMode = MutableStateFlow(false)
    val isGestureMode: StateFlow<Boolean> = _isGestureMode.asStateFlow()

    private val _currentSize = MutableStateFlow(positionManager.getOverlaySize())
    val currentSize: StateFlow<OverlaySize> = _currentSize.asStateFlow()

    private val _eyeScreenPos = MutableStateFlow(Pair(0, 0))
    val eyeScreenPos: StateFlow<Pair<Int, Int>> = _eyeScreenPos.asStateFlow()

    private var isNearLeft = false
    private var isNearRight = false
    private var eyeCenterScreenX = 0f
    private var eyeCenterScreenY = 0f

    private var lastNormalX = 0
    private var lastNormalY = 0
    private var floatingPanelHeightDp = 620

    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay(
        lifecycleOwner: LifecycleOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        savedStateRegistryOwner: SavedStateRegistryOwner
    ) {
        if (overlayComposeView != null) return

        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        val size = _currentSize.value
        val windowSizePx = (size.sizeDp * density).toInt()

        eyeController.updateScreenDimensions(windowSizePx.toFloat(), windowSizePx.toFloat())

        val (savedX, savedY) = positionManager.getPosition(
            screenWidthPx = displayMetrics.widthPixels,
            screenHeightPx = displayMetrics.heightPixels,
            windowSizePx = windowSizePx
        )

        lastNormalX = savedX
        lastNormalY = savedY
        _eyeScreenPos.value = Pair(savedX, savedY)
        eyeCenterScreenX = savedX + windowSizePx / 2f
        eyeCenterScreenY = savedY + windowSizePx / 2f
        updateScreenSideMetrics(displayMetrics.widthPixels)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            windowSizePx,
            windowSizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }
        windowLayoutParams = params

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

            setContent {
                FloatingEyeTheme {
                    OverlayEyeContent(
                        eyeController = eyeController,
                        lightController = lightController,
                        batteryController = batteryController,
                        positionManager = positionManager,
                        gestureRepository = gestureRepository,
                        installedAppsRepository = installedAppsRepository,
                        config = config,
                        micController = micController,
                        shortcutController = shortcutController,
                        gestureCaptureController = gestureCaptureController,
                        isGestureModeState = isGestureMode,
                        eyeScreenPosState = eyeScreenPos,
                        size = _currentSize.value,
                        isNearLeft = isNearLeft,
                        isNearRight = isNearRight,
                        onPlusTap = {
                            val hasAnyActive = extensionRepository.extensions.value.any { it.isActive }
                            val hasShortcuts = shortcutController.shortcuts.value.isNotEmpty()
                            if (!hasAnyActive && !hasShortcuts) {
                                Toast.makeText(context, "No modules or shortcuts active. Enable one in Module Manager.", Toast.LENGTH_SHORT).show()
                            } else {
                                val isOpen = shortcutController.toggleMenu()
                                if (isOpen) {
                                    expandWindowForRadialMenu()
                                } else {
                                    collapseWindowToDefault()
                                }
                            }
                        },
                        onPlusLongPress = {
                            if (!extensionRepository.isExtensionActive(com.example.floatingeye.extensions.repository.ExtensionRepository.ID_GESTURES)) {
                                Toast.makeText(context, "Gesture Launch is turned off in Module Manager", Toast.LENGTH_SHORT).show()
                            } else {
                                _isGestureMode.value = true
                                lightController.triggerCyanPulse()
                                gestureCaptureController.startCapture()
                                expandWindowToFullScreen()
                            }
                        },
                        onCollapseToDefault = {
                            collapseWindowToDefault()
                        },
                        onExpandFullScreen = {
                            expandWindowToFullScreen()
                        },
                        onExpandFloatingPanel = { heightDp ->
                            expandWindowForFloatingPanel(heightDp)
                        },
                        onResizeFloatingPanel = ::resizeFloatingPanel,
                        onMoveFloatingPanel = ::moveFloatingPanel,
                        onCollapseFullScreen = {
                            collapseWindowFromFullScreen()
                        },
                        onTriggerGesture = {
                            _isGestureMode.value = true
                            lightController.triggerCyanPulse()
                            gestureCaptureController.startCapture()
                            expandWindowToFullScreen()
                        },
                        onDragMove = { dx, dy ->
                            val curParams = windowLayoutParams ?: return@OverlayEyeContent
                            val screenW = displayMetrics.widthPixels
                            val screenH = displayMetrics.heightPixels
                            val curSizePx = (_currentSize.value.sizeDp * density).toInt()

                            curParams.x = (curParams.x + dx.toInt()).coerceIn(0, screenW - curSizePx)
                            curParams.y = (curParams.y + dy.toInt()).coerceIn(0, screenH - curSizePx)

                            lastNormalX = curParams.x
                            lastNormalY = curParams.y
                            _eyeScreenPos.value = Pair(curParams.x, curParams.y)
                            eyeCenterScreenX = curParams.x + curSizePx / 2f
                            eyeCenterScreenY = curParams.y + curSizePx / 2f
                            updateScreenSideMetrics(screenW)

                            try {
                                windowManager.updateViewLayout(this, curParams)
                            } catch (_: Exception) {}
                        },
                        onDragEnd = {
                            windowLayoutParams?.let {
                                positionManager.savePosition(it.x, it.y)
                            }
                        },
                        onOutsideTap = {
                            if (shortcutController.isMenuOpen.value) {
                                collapseWindowToDefault()
                                shortcutController.closeMenu()
                            }
                        },
                        onCancelGesture = {
                            collapseWindowFromFullScreen()
                            _isGestureMode.value = false
                            gestureCaptureController.clear()
                            eyeController.onTouchGazeReleased()
                        },
                        onGestureComplete = {
                            val rawPoints = gestureCaptureController.finishCapture()
                            if (rawPoints.size < 4) {
                                // Ignore noise or finger lift right after entering gesture mode; keep canvas ready
                                gestureCaptureController.clear()
                                return@OverlayEyeContent
                            }

                            val savedGestures = gestureRepository.gestures.value
                            if (savedGestures.isEmpty() || savedGestures.none { it.isEnabled }) {
                                Toast.makeText(
                                    context,
                                    "No active gestures configured. Add one in Floating Eye dashboard!",
                                    Toast.LENGTH_LONG
                                ).show()
                                collapseWindowFromFullScreen()
                                _isGestureMode.value = false
                                eyeController.onTouchGazeReleased()
                                return@OverlayEyeContent
                            }

                            val result = GestureRecognizer.recognize(rawPoints, savedGestures)
                            when (result) {
                                is RecognitionResult.Success -> {
                                    collapseWindowFromFullScreen()
                                    _isGestureMode.value = false
                                    eyeController.onTouchGazeReleased()
                                    Toast.makeText(
                                        context,
                                        "Opening ${result.binding.appName}...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    try {
                                        launchManager.launchApp(result.binding.packageName, result.binding.appName)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                is RecognitionResult.LowConfidence -> {
                                    val hint = if (result.closest != null) " (Closest: ${result.closest.appName} ${(result.confidence * 100).toInt()}%)" else ""
                                    Toast.makeText(context, "Gesture not recognized$hint. Draw again or tap (X)", Toast.LENGTH_SHORT).show()
                                    gestureCaptureController.clear()
                                }
                                RecognitionResult.InsufficientPoints -> {
                                    gestureCaptureController.clear()
                                }
                            }
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
            overlayComposeView = composeView
            _isShowing.value = true
            _isEyeVisible.value = true
            batteryController.startListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateScreenSideMetrics(screenWidth: Int) {
        isNearLeft = eyeCenterScreenX < screenWidth * 0.45f
        isNearRight = eyeCenterScreenX > screenWidth * 0.55f
    }

    private fun expandWindowToFullScreen() {
        val view = overlayComposeView ?: return
        val params = windowLayoutParams ?: return

        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.x = 0
        params.y = 0
        // Allow text fields inside floating window to receive soft keyboard focus
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()

        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun collapseWindowFromFullScreen() {
        val view = overlayComposeView ?: return
        val params = windowLayoutParams ?: return
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density

        val defaultSizePx = (_currentSize.value.sizeDp * density).toInt()
        params.width = defaultSizePx
        params.height = defaultSizePx
        params.x = lastNormalX
        params.y = lastNormalY
        // Restore non-focusable flag so background touches outside floating eye pass through
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun expandWindowForRadialMenu() {
        val view = overlayComposeView ?: return
        val params = windowLayoutParams ?: return
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density

        val expandedSizePx = (280 * density).toInt()
        val halfExpanded = expandedSizePx / 2

        val maxX = (displayMetrics.widthPixels - expandedSizePx).coerceAtLeast(0)
        val maxY = (displayMetrics.heightPixels - expandedSizePx).coerceAtLeast(0)

        params.width = expandedSizePx
        params.height = expandedSizePx
        params.x = (eyeCenterScreenX - halfExpanded).toInt().coerceIn(0, maxX)
        params.y = (eyeCenterScreenY - halfExpanded).toInt().coerceIn(0, maxY)
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun expandWindowForFloatingPanel(heightDp: Int) {
        val view = overlayComposeView ?: return
        val params = windowLayoutParams ?: return
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val widthPx = minOf((metrics.widthPixels * 0.92f).toInt(), (400 * density).toInt())
        val heightPx = minOf((heightDp * density).toInt(), (metrics.heightPixels * 0.82f).toInt())
        floatingPanelHeightDp = heightDp
        params.width = widthPx
        params.height = heightPx
        params.gravity = Gravity.TOP or Gravity.START
        params.x = (eyeCenterScreenX - widthPx / 2f).toInt().coerceIn(0, (metrics.widthPixels - widthPx).coerceAtLeast(0))
        params.y = (eyeCenterScreenY - heightPx / 2f).toInt().coerceIn(0, (metrics.heightPixels - heightPx).coerceAtLeast(0))
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv()
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun resizeFloatingPanel(minimized: Boolean) {
        val view = overlayComposeView ?: return
        val params = windowLayoutParams ?: return
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val widthPx = if (minimized) (64 * density).toInt() else minOf((metrics.widthPixels * 0.92f).toInt(), (400 * density).toInt())
        val heightPx = if (minimized) (64 * density).toInt() else minOf((floatingPanelHeightDp * density).toInt(), (metrics.heightPixels * 0.82f).toInt())
        params.width = widthPx
        params.height = heightPx
        params.x = params.x.coerceIn(0, (metrics.widthPixels - widthPx).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (metrics.heightPixels - heightPx).coerceAtLeast(0))
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun moveFloatingPanel(dx: Float, dy: Float) {
        val view = overlayComposeView ?: return
        val params = windowLayoutParams ?: return
        val metrics = context.resources.displayMetrics
        params.x = (params.x + dx.toInt()).coerceIn(0, (metrics.widthPixels - params.width).coerceAtLeast(0))
        params.y = (params.y + dy.toInt()).coerceIn(0, (metrics.heightPixels - params.height).coerceAtLeast(0))
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun collapseWindowToDefault() {
        val view = overlayComposeView ?: return
        val params = windowLayoutParams ?: return
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density

        val defaultSizePx = (_currentSize.value.sizeDp * density).toInt()
        params.width = defaultSizePx
        params.height = defaultSizePx
        params.x = lastNormalX
        params.y = lastNormalY
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    fun toggleVisibility(): Boolean {
        val view = overlayComposeView ?: return false
        val newVisibility = if (view.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        view.visibility = newVisibility
        _isEyeVisible.value = (newVisibility == View.VISIBLE)
        return _isEyeVisible.value
    }

    fun setEyeVisible(visible: Boolean) {
        val view = overlayComposeView ?: return
        view.visibility = if (visible) View.VISIBLE else View.GONE
        _isEyeVisible.value = visible
    }

    fun updateSize(newSize: OverlaySize) {
        _currentSize.value = newSize
        positionManager.saveOverlaySize(newSize)

        val view = overlayComposeView ?: return
        val params = windowLayoutParams ?: return

        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        val newWindowSizePx = (newSize.sizeDp * density).toInt()

        params.width = newWindowSizePx
        params.height = newWindowSizePx
        eyeController.updateScreenDimensions(newWindowSizePx.toFloat(), newWindowSizePx.toFloat())

        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateBloomStrength(strength: Float) {
        val safeStrength = strength.coerceIn(0f, 1f)
        positionManager.saveBloomStrength(safeStrength)
        lightController.setBloomStrength(safeStrength)
    }

    fun removeOverlay() {
        overlayComposeView?.let { view ->
            try {
                batteryController.stopListening()
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayComposeView = null
            windowLayoutParams = null
            _isShowing.value = false
        }
    }
}

@Composable
private fun OverlayEyeContent(
    eyeController: FloatingEyeController,
    lightController: FloatingEyeLightController,
    batteryController: com.example.floatingeye.battery.FloatingEyeBatteryController,
    positionManager: FloatingEyePositionManager,
    gestureRepository: GestureRepository,
    installedAppsRepository: InstalledAppsRepository,
    config: FloatingEyeConfig,
    micController: com.example.floatingeye.audio.BluetoothMicController,
    shortcutController: AppShortcutController,
    gestureCaptureController: GestureCaptureController,
    isGestureModeState: StateFlow<Boolean>,
    eyeScreenPosState: StateFlow<Pair<Int, Int>>,
    size: OverlaySize,
    isNearLeft: Boolean,
    isNearRight: Boolean,
    onPlusTap: () -> Unit,
    onPlusLongPress: () -> Unit,
    onCollapseToDefault: () -> Unit,
    onExpandFullScreen: () -> Unit = {},
    onExpandFloatingPanel: (Int) -> Unit = {},
    onResizeFloatingPanel: (Boolean) -> Unit = {},
    onMoveFloatingPanel: (Float, Float) -> Unit = { _, _ -> },
    onCollapseFullScreen: () -> Unit = {},
    onTriggerGesture: () -> Unit,
    onDragMove: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onOutsideTap: () -> Unit,
    onCancelGesture: () -> Unit,
    onGestureComplete: () -> Unit
) {
    val context = LocalContext.current
    val shortcuts by shortcutController.shortcuts.collectAsStateWithLifecycle()
    val isMenuOpen by shortcutController.isMenuOpen.collectAsStateWithLifecycle()
    val isGestureMode by isGestureModeState.collectAsStateWithLifecycle()
    val eyeScreenPos by eyeScreenPosState.collectAsStateWithLifecycle()
    val batteryInfo by batteryController.batteryInfo.collectAsStateWithLifecycle()
    val drainMonitorEnabled by batteryController.settingsRepository.drainMonitorEnabled.collectAsStateWithLifecycle()
    val drainTelemetry by batteryController.drainMonitor.telemetry.collectAsStateWithLifecycle()
    val isMicPlaying by micController.isMicPlaying.collectAsStateWithLifecycle()
    val micAudioLevel by micController.audioLevel.collectAsStateWithLifecycle()

    val neonColor by lightController.neonColor.collectAsStateWithLifecycle()
    val irisColor by lightController.irisColor.collectAsStateWithLifecycle()
    val emissivePulse by lightController.emissivePulse.collectAsStateWithLifecycle()
    val glowIntensity by lightController.glowIntensity.collectAsStateWithLifecycle()
    val bloomStrength by lightController.bloomStrength.collectAsStateWithLifecycle()

    val extensionRepository = remember {
        com.example.floatingeye.extensions.repository.ExtensionRepository(context, batteryController.settingsRepository)
    }
    val extensions by extensionRepository.extensions.collectAsStateWithLifecycle()
    val firewallRepository = remember { com.example.floatingeye.firewall.repository.FirewallRepository.getInstance(context) }
    val firewallServiceActive by firewallRepository.isFirewallActive.collectAsStateWithLifecycle()
    val isBatteryActive = extensions.firstOrNull { it.id == com.example.floatingeye.extensions.repository.ExtensionRepository.ID_BATTERY }?.isActive == true
    val isDrainActive = extensions.firstOrNull { it.id == com.example.floatingeye.extensions.repository.ExtensionRepository.ID_DRAIN_MONITOR }?.isActive == true
    val isMicActive = extensions.firstOrNull { it.id == com.example.floatingeye.extensions.repository.ExtensionRepository.ID_BLUETOOTH_MIC }?.isActive == true
    val isNotifActive = extensions.firstOrNull { it.id == com.example.floatingeye.extensions.repository.ExtensionRepository.ID_NOTIFICATIONS }?.isActive == true
    val isShortcutsActive = extensions.firstOrNull { it.id == com.example.floatingeye.extensions.repository.ExtensionRepository.ID_SHORTCUTS }?.isActive == true
    val isGesturesActive = extensions.firstOrNull { it.id == com.example.floatingeye.extensions.repository.ExtensionRepository.ID_GESTURES }?.isActive == true
    val isTasksActive = extensions.firstOrNull { it.id == com.example.floatingeye.extensions.repository.ExtensionRepository.ID_TASKS }?.isActive == true
    val isDownloaderActive = extensions.firstOrNull { it.id == com.example.floatingeye.extensions.repository.ExtensionRepository.ID_DOWNLOADER }?.isActive == true
    val isFirewallActive = extensions.firstOrNull { it.id == com.example.floatingeye.extensions.repository.ExtensionRepository.ID_FIREWALL }?.isActive == true && firewallServiceActive
    val firewallRules by firewallRepository.rules.collectAsStateWithLifecycle()
    val notificationRepository = remember { com.example.floatingeye.notifications.NotificationHistoryRepository.getInstance(context) }
    val notifications by notificationRepository.notifications.collectAsStateWithLifecycle()
    val taskRepository = remember { com.example.floatingeye.tasks.repository.TaskRepository.getInstance(context) }
    val taskList by taskRepository.tasks.collectAsStateWithLifecycle()
    val pendingTasksCount = taskList.count { !it.isCompleted }
    val downloadRepository = remember { com.example.floatingeye.downloader.repository.MediaDownloadRepository.getInstance(context) }
    val downloadList by downloadRepository.downloads.collectAsStateWithLifecycle()
    val activeDownloadsCount = downloadList.count { it.status == com.example.floatingeye.downloader.model.DownloadStatus.DOWNLOADING }

    // Active in-screen floating extension dialog state
    var activeOverlayExtensionId by remember { mutableStateOf<String?>(null) }
    var activeOverlayShortcut by remember { mutableStateOf<AppShortcut?>(null) }
    var isOverlayPanelMinimized by remember { mutableStateOf(false) }

    // Compute list of active extensions that pop up as app icons in the radial menu
    val activeExtensionItems = remember(
        extensions,
        isBatteryActive,
        isDrainActive,
        isMicActive,
        isNotifActive,
        isShortcutsActive,
        isGesturesActive,
        isTasksActive,
        isDownloaderActive,
        isFirewallActive,
        firewallRules.size,
        notifications.size,
        pendingTasksCount,
        activeDownloadsCount,
        drainTelemetry.currentMa
    ) {
        val list = mutableListOf<ActiveExtensionItem>()
        if (isFirewallActive) {
            val blockedCount = firewallRules.values.count { it.isAnyBlocked }
            list.add(
                ActiveExtensionItem(
                    id = com.example.floatingeye.extensions.repository.ExtensionRepository.ID_FIREWALL,
                    name = "Firewall",
                    iconKey = "firewall",
                    icon = Icons.Rounded.Shield,
                    badgeText = if (blockedCount > 0) "$blockedCount" else "ON"
                )
            )
        }
        if (isDownloaderActive) {
            list.add(
                ActiveExtensionItem(
                    id = com.example.floatingeye.extensions.repository.ExtensionRepository.ID_DOWNLOADER,
                    name = "Downloader",
                    iconKey = "download",
                    icon = Icons.Rounded.Download,
                    badgeText = if (activeDownloadsCount > 0) "$activeDownloadsCount" else null
                )
            )
        }
        if (isTasksActive) {
            list.add(
                ActiveExtensionItem(
                    id = com.example.floatingeye.extensions.repository.ExtensionRepository.ID_TASKS,
                    name = "Tasks",
                    iconKey = "tasks",
                    icon = Icons.Rounded.EventNote,
                    badgeText = if (pendingTasksCount > 0) "$pendingTasksCount" else null
                )
            )
        }
        if (isMicActive) {
            list.add(
                ActiveExtensionItem(
                    id = com.example.floatingeye.extensions.repository.ExtensionRepository.ID_BLUETOOTH_MIC,
                    name = "Bluetooth Mic",
                    iconKey = "mic",
                    icon = Icons.Rounded.Mic,
                    badgeText = if (isMicPlaying) "LIVE" else null
                )
            )
        }
        if (isNotifActive) {
            list.add(
                ActiveExtensionItem(
                    id = com.example.floatingeye.extensions.repository.ExtensionRepository.ID_NOTIFICATIONS,
                    name = "Notification Sentinel",
                    iconKey = "notifications",
                    icon = Icons.Rounded.NotificationsActive,
                    badgeText = if (notifications.isNotEmpty()) "${notifications.size}" else null
                )
            )
        }
        if (isBatteryActive) {
            list.add(
                ActiveExtensionItem(
                    id = com.example.floatingeye.extensions.repository.ExtensionRepository.ID_BATTERY,
                    name = "Battery Reactions",
                    iconKey = "battery",
                    icon = Icons.Rounded.BatteryChargingFull,
                    badgeText = "${batteryInfo.percentage}%"
                )
            )
        }
        if (isDrainActive) {
            list.add(
                ActiveExtensionItem(
                    id = com.example.floatingeye.extensions.repository.ExtensionRepository.ID_DRAIN_MONITOR,
                    name = "Drain Monitor",
                    iconKey = "drain",
                    icon = Icons.Rounded.Speed,
                    badgeText = if (drainTelemetry.hasCurrentReading) "${drainTelemetry.currentMa}mA" else "N/A"
                )
            )
        }
        if (isGesturesActive) {
            list.add(
                ActiveExtensionItem(
                    id = com.example.floatingeye.extensions.repository.ExtensionRepository.ID_GESTURES,
                    name = "Gestures",
                    iconKey = "gestures",
                    icon = Icons.Rounded.Gesture
                )
            )
        }
        // Appearance or Orbit if activated
        extensions.filter { it.isActive && it.id !in listOf(
            com.example.floatingeye.extensions.repository.ExtensionRepository.ID_SHORTCUTS,
            com.example.floatingeye.extensions.repository.ExtensionRepository.ID_BLUETOOTH_MIC,
            com.example.floatingeye.extensions.repository.ExtensionRepository.ID_NOTIFICATIONS,
            com.example.floatingeye.extensions.repository.ExtensionRepository.ID_BATTERY,
            com.example.floatingeye.extensions.repository.ExtensionRepository.ID_DRAIN_MONITOR,
            com.example.floatingeye.extensions.repository.ExtensionRepository.ID_GESTURES,
            com.example.floatingeye.extensions.repository.ExtensionRepository.ID_TASKS,
            com.example.floatingeye.extensions.repository.ExtensionRepository.ID_DOWNLOADER,
            com.example.floatingeye.extensions.repository.ExtensionRepository.ID_FIREWALL
        ) }.forEach { ext ->
            val icon = when (ext.id) {
                com.example.floatingeye.extensions.repository.ExtensionRepository.ID_APPEARANCE -> Icons.Rounded.Palette
                else -> Icons.Rounded.Widgets
            }
            list.add(
                ActiveExtensionItem(
                    id = ext.id,
                    name = ext.name,
                    iconKey = ext.iconKey,
                    icon = icon
                )
            )
        }
        list
    }

    if (activeOverlayShortcut != null) {
        val shortcut = activeOverlayShortcut!!
        FloatingAppShortcutWindow(
            shortcut = shortcut,
            isMinimized = isOverlayPanelMinimized,
            onRestore = {
                isOverlayPanelMinimized = false
                onResizeFloatingPanel(false)
            },
            onMinimize = {
                isOverlayPanelMinimized = true
                onResizeFloatingPanel(true)
            },
            onMaximize = {
                activeOverlayShortcut = null
                isOverlayPanelMinimized = false
                onCollapseFullScreen()
                shortcutController.launchShortcut(shortcut)
            },
            onClose = {
                activeOverlayShortcut = null
                isOverlayPanelMinimized = false
                onCollapseFullScreen()
            },
            onDrag = onMoveFloatingPanel
        )
    } else if (activeOverlayExtensionId != null) {
        FloatingExtensionWindow(
            activeExtensionId = activeOverlayExtensionId,
            onClose = {
                activeOverlayExtensionId = null
                onCollapseFullScreen()
            },
            onMinimizedChanged = onResizeFloatingPanel,
            onDrag = onMoveFloatingPanel,
            onOpenFullApp = { extId ->
                activeOverlayExtensionId = null
                onCollapseFullScreen()
                val intent = Intent(context, com.example.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("open_extension_id", extId)
                }
                context.startActivity(intent)
            },
            onSwitchExtension = { extId -> activeOverlayExtensionId = extId },
            taskRepository = taskRepository,
            mediaDownloadRepository = downloadRepository,
            notificationRepository = notificationRepository,
            batterySettingsRepository = batteryController.settingsRepository,
            batteryController = batteryController,
            micController = micController,
            positionManager = positionManager,
            gestureRepository = gestureRepository,
            installedAppsRepository = installedAppsRepository,
            config = config,
            onFirewallActiveChanged = { active ->
                extensionRepository.toggleActive(
                    com.example.floatingeye.extensions.repository.ExtensionRepository.ID_FIREWALL,
                    active
                )
            },
            onRequestFirewallEnable = {
                activeOverlayExtensionId = null
                onCollapseFullScreen()
                val intent = Intent(context, com.example.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("open_extension_id", com.example.floatingeye.extensions.repository.ExtensionRepository.ID_FIREWALL)
                    putExtra("request_firewall_vpn", true)
                }
                context.startActivity(intent)
            },
            firewallRepository = firewallRepository
        )
    } else if (isGestureMode) {
        // Full screen transparent canvas during active gesture drawing
        Box(modifier = Modifier.fillMaxSize()) {
            // Live cyan stroke drawing canvas
            GestureDrawingCanvas(
                captureController = gestureCaptureController,
                onPointerMove = { px, py ->
                    eyeController.onTouchGaze(px, py)
                },
                onGestureComplete = onGestureComplete,
                modifier = Modifier.fillMaxSize()
            )

            // Hint HUD pill with cancel button at top
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 14.dp, top = 4.dp, end = 6.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "Draw gesture to launch app",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanNeonPrimary
                    )
                    IconButton(
                        onClick = onCancelGesture,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel Gesture Mode",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Floating Plus button positioned at its exact current screen coordinates
            Box(
                modifier = Modifier
                    .offset { IntOffset(eyeScreenPos.first, eyeScreenPos.second) }
                    .size(size.sizeDp.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingEyePlusWidget(
                    size = size.sizeDp.dp,
                    isOpen = false,
                    isCharging = batteryInfo.isCharging,
                    isFull = batteryInfo.isFull,
                    batteryPercentage = batteryInfo.percentage,
                    neonColor = neonColor,
                    irisColor = irisColor,
                    emissivePulse = emissivePulse,
                    glowIntensity = glowIntensity,
                    bloomStrength = bloomStrength,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        // Normal floating / radial menu state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onOutsideTap()
                },
            contentAlignment = Alignment.Center
        ) {
            // Radial shortcuts and active extensions mounted like apps orbiting the floating eye
            RadialAppShortcutMenu(
                shortcuts = if (isShortcutsActive) shortcuts else emptyList(),
                isOpen = isMenuOpen,
                isNearLeft = isNearLeft,
                isNearRight = isNearRight,
                activeExtensions = activeExtensionItems,
                onActiveExtensionClick = { extItem ->
                    activeOverlayShortcut = null
                    isOverlayPanelMinimized = false
                    shortcutController.closeMenu()
                    activeOverlayExtensionId = extItem.id
                    onExpandFloatingPanel(620)
                },
                isMicPlaying = isMicPlaying,
                micAudioLevel = micAudioLevel,
                onToggleMic = if (isMicActive) {
                    {
                        if (!micController.hasRecordPermission()) {
                            Toast.makeText(
                                context,
                                "Microphone permission required. Open Hiris Controller to grant.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val active = micController.toggleStreaming()
                            Toast.makeText(
                                context,
                                if (active) "Live mic enabled! (Bluetooth Streaming)" else "Live mic stopped",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else null,
                notificationCount = if (isNotifActive) notifications.size else 0,
                onOpenNotifications = if (isNotifActive) {
                    {
                        activeOverlayShortcut = null
                        isOverlayPanelMinimized = false
                        shortcutController.closeMenu()
                        activeOverlayExtensionId = com.example.floatingeye.extensions.repository.ExtensionRepository.ID_NOTIFICATIONS
                        onExpandFloatingPanel(620)
                    }
                } else null,
                onLaunchShortcut = { shortcut ->
                    activeOverlayExtensionId = null
                    activeOverlayShortcut = shortcut
                    isOverlayPanelMinimized = false
                    shortcutController.closeMenu()
                    onExpandFloatingPanel(240)
                }
            )

            // The Floating Plus Button with direct unified one-click tap, drag, and long-press
            val viewConfig = LocalViewConfiguration.current
            Box(
                modifier = Modifier
                    .size(size.sizeDp.dp)
                    .pointerInput(isMenuOpen, isShortcutsActive, isGesturesActive, activeExtensionItems.size) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startX = down.position.x
                            val startY = down.position.y
                            val startTime = System.currentTimeMillis()
                            var hasDragged = false
                            var longPressed = false
                            val longPressTimeout = 380L
                            val touchSlop = viewConfig.touchSlop

                            while (true) {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!pointer.pressed) {
                                    pointer.consume()
                                    break
                                }

                                val dx = pointer.position.x - startX
                                val dy = pointer.position.y - startY
                                val dist = kotlin.math.hypot(dx, dy)

                                if (!hasDragged && !longPressed && dist > touchSlop) {
                                    hasDragged = true
                                    if (isMenuOpen) {
                                        onOutsideTap()
                                    }
                                }

                                if (!hasDragged && !longPressed && (System.currentTimeMillis() - startTime) >= longPressTimeout) {
                                    longPressed = true
                                    pointer.consume()
                                    if (isGesturesActive) {
                                        onPlusLongPress()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Gesture Launch is disabled. Enable it in Module Manager.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    break
                                }

                                if (hasDragged) {
                                    pointer.consume()
                                    val prev = pointer.previousPosition
                                    val moveDx = pointer.position.x - prev.x
                                    val moveDy = pointer.position.y - prev.y
                                    onDragMove(moveDx, moveDy)
                                }
                            }

                            if (hasDragged) {
                                onDragEnd()
                            } else if (!longPressed) {
                                // ONE CLICK (TAP): Open radial menu showing active extensions and shortcuts!
                                // Let the controller refresh its repository first. Its state can
                                // otherwise lag behind extension switches changed in MainActivity.
                                onPlusTap()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                FloatingEyePlusWidget(
                    size = size.sizeDp.dp,
                    isOpen = isMenuOpen,
                    isCharging = batteryInfo.isCharging && isBatteryActive,
                    isFull = batteryInfo.isFull && isBatteryActive,
                    batteryPercentage = batteryInfo.percentage,
                    neonColor = neonColor,
                    irisColor = irisColor,
                    emissivePulse = emissivePulse,
                    glowIntensity = glowIntensity,
                    bloomStrength = bloomStrength,
                    modifier = Modifier.fillMaxSize()
                )

                // Optional Cybernetic Real-time Drain Telemetry Badge
                // ONLY shown when drainMonitorEnabled and drain extension are activated by the user
                if (drainMonitorEnabled && isDrainActive && !isMenuOpen) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xDD060A14),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeonPrimary.copy(alpha = 0.65f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = if (drainTelemetry.isCharging) BatteryColors.NeonElectricCyan else Color(0xFFFFB020),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = when {
                                    !drainTelemetry.hasCurrentReading -> "N/A"
                                    drainTelemetry.isCharging -> "+${drainTelemetry.currentMa}mA"
                                    else -> "${drainTelemetry.currentMa}mA"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
