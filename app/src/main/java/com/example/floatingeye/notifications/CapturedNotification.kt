package com.example.floatingeye.notifications

/**
 * Data model for a captured system notification and its locally saved media attachments.
 * Stored locally in Hiris Controller so even if the sender un-sends or deletes the message,
 * the content and media remain preserved on device.
 */
data class CapturedNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val mediaPath: String? = null,
    val mediaType: String? = null, // "image", "audio", "video"
    val channelId: String? = null,
    val senderName: String? = null,
    val isMediaCaptured: Boolean = !mediaPath.isNullOrEmpty()
)
