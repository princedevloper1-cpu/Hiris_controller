package com.example.floatingeye.shortcuts

import android.graphics.drawable.Drawable

/**
 * Data model for user-configured Floating Eye app shortcuts.
 */
data class AppShortcut(
    val packageName: String,
    val displayName: String,
    val iconReference: String = "",
    val order: Int = 0
)

/**
 * Metadata for apps installed on the user's Android device.
 */
data class InstalledAppInfo(
    val packageName: String,
    val displayName: String,
    val icon: Drawable? = null
)
