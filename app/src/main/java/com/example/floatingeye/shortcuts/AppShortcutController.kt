package com.example.floatingeye.shortcuts

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin

data class RadialIconPosition(
    val shortcut: AppShortcut,
    val centerOffset: Offset,
    val angleDegrees: Double
)

class AppShortcutController(
    private val repository: AppShortcutRepository,
    private val launchManager: AppLaunchManager
) {
    private val _isMenuOpen = MutableStateFlow(false)
    val isMenuOpen: StateFlow<Boolean> = _isMenuOpen.asStateFlow()

    private val _focusedShortcut = MutableStateFlow<AppShortcut?>(null)
    val focusedShortcut: StateFlow<AppShortcut?> = _focusedShortcut.asStateFlow()

    val shortcuts: StateFlow<List<AppShortcut>> = repository.shortcuts

    fun toggleMenu(): Boolean {
        _isMenuOpen.value = !_isMenuOpen.value
        if (!_isMenuOpen.value) {
            _focusedShortcut.value = null
        }
        return _isMenuOpen.value
    }

    fun openMenu() {
        _isMenuOpen.value = true
    }

    fun closeMenu() {
        _isMenuOpen.value = false
        _focusedShortcut.value = null
    }

    fun setFocusedShortcut(shortcut: AppShortcut?) {
        _focusedShortcut.value = shortcut
    }

    fun launchShortcut(shortcut: AppShortcut): Boolean {
        val success = launchManager.launchApp(shortcut.packageName, shortcut.displayName)
        closeMenu()
        return success
    }

    fun launchShortcutAdjacent(shortcut: AppShortcut): Boolean {
        val success = launchManager.launchAppAdjacent(shortcut.packageName, shortcut.displayName)
        closeMenu()
        return success
    }

    /**
     * Calculates curved radial positions for each shortcut around the eye center.
     * Guaranteed never to place icons outside the visible screen bounds.
     */
    fun calculateRadialPositions(
        eyeCenterX: Float,
        eyeCenterY: Float,
        screenWidthPx: Float,
        screenHeightPx: Float,
        radialDistancePx: Float,
        iconSizePx: Float
    ): List<RadialIconPosition> {
        val currentShortcuts = shortcuts.value
        if (currentShortcuts.isEmpty()) return emptyList()

        val count = currentShortcuts.size
        val isNearLeft = eyeCenterX < screenWidthPx * 0.45f
        val isNearRight = eyeCenterX > screenWidthPx * 0.55f

        // Determine base direction angle (0 deg = East/Right, 90 deg = South/Down, 180 deg = West/Left, 270 deg = North/Up)
        val (baseAngle, spreadAngle) = when {
            isNearLeft -> Pair(0.0, 140.0) // Fan towards right (-70 to +70)
            isNearRight -> Pair(180.0, 140.0) // Fan towards left (110 to 250)
            eyeCenterY < screenHeightPx * 0.35f -> Pair(90.0, 140.0) // Fan downwards
            else -> Pair(270.0, 140.0) // Fan upwards
        }

        val angles = when (count) {
            1 -> listOf(baseAngle)
            else -> {
                val start = baseAngle - spreadAngle / 2.0
                val step = spreadAngle / (count - 1).toDouble()
                (0 until count).map { i -> start + i * step }
            }
        }

        val halfIcon = iconSizePx / 2f
        val margin = 8f

        return currentShortcuts.mapIndexed { index, shortcut ->
            val angleDeg = angles[index]
            val angleRad = Math.toRadians(angleDeg)

            val rawX = eyeCenterX + (radialDistancePx * cos(angleRad)).toFloat()
            val rawY = eyeCenterY + (radialDistancePx * sin(angleRad)).toFloat()

            // Strictly prevent icons from clipping outside visible screen
            val clampedX = rawX.coerceIn(halfIcon + margin, screenWidthPx - halfIcon - margin)
            val clampedY = rawY.coerceIn(halfIcon + margin, screenHeightPx - halfIcon - margin)

            RadialIconPosition(
                shortcut = shortcut,
                centerOffset = Offset(clampedX, clampedY),
                angleDegrees = angleDeg
            )
        }
    }
}
