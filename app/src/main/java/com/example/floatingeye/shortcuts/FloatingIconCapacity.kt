package com.example.floatingeye.shortcuts

import android.content.Context

/** Shared capacity rules for app shortcuts and extensions shown around the Floating Eye. */
object FloatingIconCapacity {
    const val MAX_ITEMS = 8

    private const val EXTENSION_PREFS = "hiris_extensions_prefs"
    private const val SHORTCUT_PREFS = "floating_eye_shortcuts_prefs"
    private const val SHORTCUTS_KEY = "selected_app_shortcuts_json"

    // These extensions have controls on the orbit. Shortcuts and appearance settings do not.
    private val orbitExtensionIds = setOf(
        "gestures", "battery", "drain_monitor", "mic", "notifications",
        "tasks", "hiris_capture", "firewall"
    )

    fun activeExtensionCount(context: Context): Int {
        val prefs = context.getSharedPreferences(EXTENSION_PREFS, Context.MODE_PRIVATE)
        return orbitExtensionIds.count { id -> prefs.getBoolean("ext_active_$id", false) }
    }

    fun savedShortcutCount(context: Context): Int {
        val raw = context.getSharedPreferences(SHORTCUT_PREFS, Context.MODE_PRIVATE)
            .getString(SHORTCUTS_KEY, null)
        return runCatching { org.json.JSONArray(raw ?: "[]").length() }.getOrDefault(0)
    }

    fun canAddExtension(context: Context, extensionId: String): Boolean =
        extensionId !in orbitExtensionIds ||
            savedShortcutCount(context) + activeExtensionCount(context) < MAX_ITEMS

    fun canAddShortcut(context: Context): Boolean =
        savedShortcutCount(context) + activeExtensionCount(context) < MAX_ITEMS

    fun availableShortcutSlots(context: Context): Int =
        (MAX_ITEMS - activeExtensionCount(context)).coerceAtLeast(0)
}
