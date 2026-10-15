package com.example.floatingeye.overlay

import android.content.Context
import android.content.SharedPreferences

enum class OverlaySize(val label: String, val sizeDp: Int) {
    SMALL("Small", 68),
    MEDIUM("Medium (Default)", 80),
    LARGE("Large", 96)
}

class FloatingEyePositionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun savePosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_POS_X, x)
            .putInt(KEY_POS_Y, y)
            .putBoolean(KEY_HAS_SAVED_POS, true)
            .apply()
    }

    fun getPosition(screenWidthPx: Int, screenHeightPx: Int, windowSizePx: Int): Pair<Int, Int> {
        val hasSaved = prefs.getBoolean(KEY_HAS_SAVED_POS, false)
        if (!hasSaved) {
            // Default position: top-right area, nicely placed
            val defaultX = (screenWidthPx - windowSizePx - 40).coerceAtLeast(0)
            val defaultY = (screenHeightPx * 0.22f).toInt()
            return Pair(defaultX, defaultY)
        }

        val rawX = prefs.getInt(KEY_POS_X, 0)
        val rawY = prefs.getInt(KEY_POS_Y, 200)

        // Ensure within current screen bounds
        val maxX = (screenWidthPx - windowSizePx).coerceAtLeast(0)
        val maxY = (screenHeightPx - windowSizePx).coerceAtLeast(0)

        val clampedX = rawX.coerceIn(0, maxX)
        val clampedY = rawY.coerceIn(0, maxY)

        return Pair(clampedX, clampedY)
    }

    fun saveOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()
    }

    fun isOverlayEnabled(): Boolean {
        return prefs.getBoolean(KEY_OVERLAY_ENABLED, false)
    }

    fun saveOverlaySize(size: OverlaySize) {
        prefs.edit().putString(KEY_OVERLAY_SIZE, size.name).apply()
    }

    fun getOverlaySize(): OverlaySize {
        val sizeName = prefs.getString(KEY_OVERLAY_SIZE, OverlaySize.MEDIUM.name)
        return try {
            OverlaySize.valueOf(sizeName ?: OverlaySize.MEDIUM.name)
        } catch (_: Exception) {
            OverlaySize.MEDIUM
        }
    }

    fun saveBloomStrength(strength: Float) {
        prefs.edit().putFloat(KEY_BLOOM_STRENGTH, strength.coerceIn(0f, 1f)).apply()
    }

    fun getBloomStrength(defaultValue: Float = 0.35f): Float =
        prefs.getFloat(KEY_BLOOM_STRENGTH, defaultValue).coerceIn(0f, 1f)

    fun saveGlowIntensity(intensity: Float) {
        prefs.edit().putFloat(KEY_GLOW_INTENSITY, intensity.coerceIn(0.5f, 2.5f)).apply()
    }

    fun getGlowIntensity(defaultValue: Float = 1f): Float =
        prefs.getFloat(KEY_GLOW_INTENSITY, defaultValue).coerceIn(0.5f, 2.5f)

    companion object {
        private const val PREFS_NAME = "floating_eye_overlay_prefs"
        private const val KEY_POS_X = "overlay_position_x"
        private const val KEY_POS_Y = "overlay_position_y"
        private const val KEY_HAS_SAVED_POS = "has_saved_position"
        private const val KEY_OVERLAY_ENABLED = "overlay_is_enabled"
        private const val KEY_OVERLAY_SIZE = "overlay_size"
        private const val KEY_BLOOM_STRENGTH = "overlay_bloom_strength"
        private const val KEY_GLOW_INTENSITY = "overlay_glow_intensity"
    }
}
