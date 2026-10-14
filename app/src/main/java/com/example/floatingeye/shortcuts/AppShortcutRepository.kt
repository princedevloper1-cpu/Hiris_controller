package com.example.floatingeye.shortcuts

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages persistence for up to 6 user-selected app shortcuts.
 */
class AppShortcutRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _shortcuts = MutableStateFlow<List<AppShortcut>>(emptyList())
    val shortcuts: StateFlow<List<AppShortcut>> = _shortcuts.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_SHORTCUTS) loadShortcuts()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        loadShortcuts()
        migrateLegacyAutoSeededShortcuts()
    }

    private fun loadShortcuts() {
        val jsonString = prefs.getString(KEY_SHORTCUTS, null)
        if (jsonString.isNullOrEmpty()) {
            _shortcuts.value = emptyList()
            return
        }

        try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<AppShortcut>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(
                    AppShortcut(
                        packageName = item.getString("packageName"),
                        displayName = item.getString("displayName"),
                        iconReference = item.optString("iconReference", ""),
                        order = item.optInt("order", i)
                    )
                )
            }
            _shortcuts.value = list.sortedBy { it.order }.take(MAX_SHORTCUTS)
        } catch (e: Exception) {
            _shortcuts.value = emptyList()
        }
    }

    /** Removes only the exact app list this repository used to add automatically. */
    private fun migrateLegacyAutoSeededShortcuts() {
        if (prefs.getBoolean(KEY_LEGACY_DEFAULTS_MIGRATED, false)) return

        val savedPackages = _shortcuts.value.map { it.packageName }
        val oldAutoSeededPackages = findLegacyAutoSeededShortcuts().map { it.packageName }
        if (oldAutoSeededPackages.isNotEmpty() && savedPackages == oldAutoSeededPackages) {
            prefs.edit().remove(KEY_SHORTCUTS).apply()
            _shortcuts.value = emptyList()
        }

        prefs.edit().putBoolean(KEY_LEGACY_DEFAULTS_MIGRATED, true).apply()
    }

    private fun findLegacyAutoSeededShortcuts(): List<AppShortcut> {
        return try {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = context.packageManager.queryIntentActivities(launcherIntent, 0)
            val defaults = mutableListOf<AppShortcut>()
            val keywords = listOf(
                "camera", "setting", "browser", "chrome", "message",
                "dialer", "phone", "gallery", "clock", "calculator"
            )

            for (info in apps) {
                val packageName = info.activityInfo.packageName
                if (packageName == context.packageName) continue
                val label = info.loadLabel(context.packageManager)?.toString() ?: packageName
                if (keywords.any { label.contains(it, ignoreCase = true) || packageName.contains(it, ignoreCase = true) }) {
                    if (defaults.none { it.packageName == packageName }) {
                        defaults.add(AppShortcut(packageName, label, order = defaults.size))
                    }
                }
                if (defaults.size >= 4) break
            }

            if (defaults.size < 4) {
                for (info in apps) {
                    val packageName = info.activityInfo.packageName
                    if (packageName == context.packageName || defaults.any { it.packageName == packageName }) continue
                    val label = info.loadLabel(context.packageManager)?.toString() ?: packageName
                    defaults.add(AppShortcut(packageName, label, order = defaults.size))
                    if (defaults.size >= 4) break
                }
            }
            defaults
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<AppShortcut>) {
        val jsonArray = JSONArray()
        list.sortedBy { it.order }.take(MAX_SHORTCUTS).forEachIndexed { idx, sc ->
            val obj = JSONObject().apply {
                put("packageName", sc.packageName)
                put("displayName", sc.displayName)
                put("iconReference", sc.iconReference)
                put("order", idx)
            }
            jsonArray.put(obj)
        }

        prefs.edit().putString(KEY_SHORTCUTS, jsonArray.toString()).apply()
        loadShortcuts()
    }

    fun addShortcut(shortcut: AppShortcut): Boolean {
        val current = _shortcuts.value.toMutableList()
        if (current.any { it.packageName == shortcut.packageName }) {
            return false // Already added
        }
        if (current.size >= MAX_SHORTCUTS || !FloatingIconCapacity.canAddShortcut(context)) {
            return false // Limit reached
        }

        current.add(shortcut.copy(order = current.size))
        persist(current)
        return true
    }

    fun removeShortcut(packageName: String) {
        val current = _shortcuts.value.filter { it.packageName != packageName }
        persist(current)
    }

    fun moveUp(index: Int) {
        val current = _shortcuts.value.toMutableList()
        if (index > 0 && index < current.size) {
            val temp = current[index]
            current[index] = current[index - 1]
            current[index - 1] = temp
            persist(current)
        }
    }

    fun moveDown(index: Int) {
        val current = _shortcuts.value.toMutableList()
        if (index >= 0 && index < current.size - 1) {
            val temp = current[index]
            current[index] = current[index + 1]
            current[index + 1] = temp
            persist(current)
        }
    }

    fun reorder(newList: List<AppShortcut>) {
        persist(newList)
    }

    companion object {
        const val MAX_SHORTCUTS = FloatingIconCapacity.MAX_ITEMS
        private const val PREFS_NAME = "floating_eye_shortcuts_prefs"
        private const val KEY_SHORTCUTS = "selected_app_shortcuts_json"
        private const val KEY_LEGACY_DEFAULTS_MIGRATED = "legacy_auto_seeded_shortcuts_migrated"
    }
}
