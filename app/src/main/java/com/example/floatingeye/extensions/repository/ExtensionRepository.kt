package com.example.floatingeye.extensions.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.floatingeye.battery.BatterySettingsRepository
import com.example.floatingeye.extensions.model.ExtensionCategory
import com.example.floatingeye.extensions.model.HirisExtension
import com.example.floatingeye.notifications.NotificationPermissionHelper
import com.example.floatingeye.notifications.NotificationHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExtensionRepository(
    private val context: Context,
    private val batterySettingsRepository: BatterySettingsRepository? = null,
    private val notificationRepository: NotificationHistoryRepository? = null
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Every extension starts as INACTIVE (false) by default.
    // Switches are never static and never automatically enabled:
    // the user must explicitly toggle the switch for a feature to become active!
    private val defaultExtensions = listOf(
        HirisExtension(
            id = ID_SHORTCUTS,
            name = "Shortcuts",
            description = "Radial app launcher around HIRIS.",
            categoryTags = listOf("Productivity", "Launcher"),
            category = ExtensionCategory.PRODUCTIVITY,
            iconKey = "shortcuts",
            isInstalled = true,
            isActive = false // OFF by default; user flips switch to activate
        ),
        HirisExtension(
            id = ID_GESTURES,
            name = "Gesture Launch",
            description = "Draw a symbol to open an app.",
            categoryTags = listOf("Gestures", "Productivity"),
            category = ExtensionCategory.PRODUCTIVITY,
            iconKey = "gestures",
            isInstalled = true,
            isActive = false // OFF by default; user flips switch to activate
        ),
        HirisExtension(
            id = ID_BATTERY,
            name = "Battery Reactions",
            description = "Light behavior based on battery state.",
            categoryTags = listOf("Visualization", "System"),
            category = ExtensionCategory.VISUALIZATION,
            iconKey = "battery",
            isInstalled = true,
            isActive = false // OFF by default; user flips switch to activate
        ),
        HirisExtension(
            id = ID_DRAIN_MONITOR,
            name = "Power Drain Monitor",
            description = "Estimate live battery impact of active apps.",
            categoryTags = listOf("System", "Monitoring"),
            category = ExtensionCategory.SYSTEM,
            iconKey = "drain",
            isInstalled = true,
            isActive = false // OFF by default; user flips switch to activate
        ),
        HirisExtension(
            id = ID_APPEARANCE,
            name = "Appearance Pack",
            description = "Set the floating eye size and glow intensity.",
            categoryTags = listOf("Customization", "Size", "Glow"),
            category = ExtensionCategory.CUSTOMIZATION,
            iconKey = "appearance",
            isInstalled = true,
            isActive = false // OFF by default; user flips switch to activate
        ),
        HirisExtension(
            id = ID_BLUETOOTH_MIC,
            name = "Bluetooth Mic",
            description = "Live audio loopback & streaming from HIRIS.",
            categoryTags = listOf("Audio", "Utilities"),
            category = ExtensionCategory.UTILITIES,
            iconKey = "mic",
            isInstalled = true,
            isActive = false // OFF by default; user flips switch to activate
        ),
        HirisExtension(
            id = ID_NOTIFICATIONS,
            name = "Notification Sentinel",
            description = "Capture & preserve incoming system notifications.",
            categoryTags = listOf("System", "Utilities"),
            category = ExtensionCategory.UTILITIES,
            iconKey = "notifications",
            isInstalled = true,
            isActive = false // OFF by default; user flips switch to activate
        ),
        HirisExtension(
            id = ID_TASKS,
            name = "Tasks & Reminders",
            description = "Plan tasks, choose date & time, and receive smart alarms.",
            categoryTags = listOf("Productivity", "Reminders", "Tasks"),
            category = ExtensionCategory.PRODUCTIVITY,
            iconKey = "tasks",
            isInstalled = true,
            isActive = false, // OFF by default; user flips switch to activate
            isNew = true
        ),
        HirisExtension(
            id = ID_DOWNLOADER,
            name = "Media Downloader",
            description = "Download videos & audio with direct links rapidly.",
            categoryTags = listOf("Media", "Video", "Audio", "Downloader"),
            category = ExtensionCategory.UTILITIES,
            iconKey = "download",
            isInstalled = true,
            isActive = false, // OFF by default; user flips switch to activate
            isNew = true
        ),
        HirisExtension(
            id = ID_FIREWALL,
            name = "Firewall & App Blocker",
            description = "Block mobile data or Wi-Fi for apps like WhatsApp & Facebook to stop background data.",
            categoryTags = listOf("Network", "Security", "Firewall"),
            category = ExtensionCategory.SYSTEM,
            iconKey = "firewall",
            isInstalled = true,
            isActive = false, // OFF by default; user flips switch to activate
            isNew = true
        )
    )

    private val _extensions = MutableStateFlow<List<HirisExtension>>(loadExtensions())
    val extensions: StateFlow<List<HirisExtension>> = _extensions.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key.startsWith("ext_active_") || key.startsWith("ext_installed_")) {
            _extensions.value = loadExtensions()
        }
    }

    init {
        // The app screen and floating overlay each own a repository instance.
        // Keep both views in sync when either one changes an extension switch.
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        // The extension switch is the source of truth for these runtime features.
        if (isExtensionActive(ID_NOTIFICATIONS) &&
            !NotificationPermissionHelper.isNotificationAccessGranted(context)
        ) {
            toggleActive(ID_NOTIFICATIONS, false)
        }
        batterySettingsRepository?.setReactionsEnabled(isExtensionActive(ID_BATTERY))
        notificationRepository?.setCaptureEnabled(isExtensionActive(ID_NOTIFICATIONS))
    }

    private fun loadExtensions(): List<HirisExtension> {
        return defaultExtensions.map { def ->
            val installedKey = "ext_installed_${def.id}"
            val activeKey = "ext_active_${def.id}"

            val isInstalled = if (prefs.contains(installedKey)) {
                prefs.getBoolean(installedKey, def.isInstalled)
            } else {
                def.isInstalled
            }

            val isActive = if (prefs.contains(activeKey)) {
                prefs.getBoolean(activeKey, false)
            } else {
                false
            }

            def.copy(
                isInstalled = isInstalled,
                isActive = if (isInstalled) isActive else false
            )
        }
    }

    fun isExtensionActive(extensionId: String): Boolean {
        val ext = _extensions.value.firstOrNull { it.id == extensionId }
        return ext != null && ext.isInstalled && ext.isActive
    }

    fun toggleActive(extensionId: String, active: Boolean) {
        val extension = _extensions.value.firstOrNull { it.id == extensionId } ?: return
        if (active && !extension.isInstalled) return

        prefs.edit().putBoolean("ext_active_$extensionId", active).apply()

        // Sync with underlying subsystems immediately
        when (extensionId) {
            ID_BATTERY -> batterySettingsRepository?.setReactionsEnabled(active)
            ID_DRAIN_MONITOR -> batterySettingsRepository?.setDrainMonitorEnabled(active)
            ID_BLUETOOTH_MIC -> if (!active) com.example.floatingeye.audio.BluetoothMicController.getInstance(context).stopStreaming()
            ID_NOTIFICATIONS -> notificationRepository?.setCaptureEnabled(active)
            ID_FIREWALL -> com.example.floatingeye.firewall.repository.FirewallRepository.getInstance(context).setFirewallActive(active)
        }

        _extensions.value = _extensions.value.map {
            if (it.id == extensionId) {
                it.copy(isActive = active && it.isInstalled)
            } else it
        }
    }

    fun installExtension(extensionId: String) {
        if (_extensions.value.none { it.id == extensionId }) return

        prefs.edit()
            .putBoolean("ext_installed_$extensionId", true)
            .putBoolean("ext_active_$extensionId", false)
            .apply()

        _extensions.value = _extensions.value.map {
            if (it.id == extensionId) {
                it.copy(isInstalled = true, isActive = false, isNew = false)
            } else it
        }
    }

    fun uninstallExtension(extensionId: String) {
        if (_extensions.value.none { it.id == extensionId }) return

        // Stop any runtime feature before clearing its installed/active state.
        toggleActive(extensionId, false)
        prefs.edit()
            .putBoolean("ext_installed_$extensionId", false)
            .putBoolean("ext_active_$extensionId", false)
            .apply()

        _extensions.value = _extensions.value.map {
            if (it.id == extensionId) {
                it.copy(isInstalled = false, isActive = false)
            } else it
        }
    }

    companion object {
        private const val PREFS_NAME = "hiris_extensions_prefs"

        const val ID_SHORTCUTS = "shortcuts"
        const val ID_GESTURES = "gestures"
        const val ID_BATTERY = "battery"
        const val ID_DRAIN_MONITOR = "drain_monitor"
        const val ID_APPEARANCE = "appearance"
        const val ID_BLUETOOTH_MIC = "mic"
        const val ID_NOTIFICATIONS = "notifications"
        const val ID_TASKS = "tasks"
        const val ID_DOWNLOADER = "downloader"
        const val ID_FIREWALL = "firewall"
    }
}
