package com.example.floatingeye.notifications

import android.app.Notification
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Android NotificationListenerService that captures incoming notifications,
 * chat messages from social networks (WhatsApp, Messenger, Telegram, Instagram, SMS),
 * and attached media (photos, voice notes, video clips).
 */
class HirisNotificationListenerService : NotificationListenerService() {

    private lateinit var repository: NotificationHistoryRepository

    companion object {
        @Volatile
        private var connectedService: HirisNotificationListenerService? = null

        fun captureActiveNotificationsIfConnected() {
            connectedService?.captureCurrentActiveNotifications()
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = NotificationHistoryRepository.getInstance(applicationContext)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        connectedService = this
        if (!::repository.isInitialized) {
            repository = NotificationHistoryRepository.getInstance(applicationContext)
        }
        captureCurrentActiveNotifications()
    }

    override fun onListenerDisconnected() {
        if (connectedService === this) connectedService = null
        super.onListenerDisconnected()
    }

    private fun captureCurrentActiveNotifications() {
        if (!::repository.isInitialized || !repository.isCaptureEnabled.value) return
        try {
            activeNotifications?.forEach(::captureNotification)
        } catch (_: SecurityException) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        captureNotification(sbn)
    }

    @Suppress("DEPRECATION")
    private fun captureNotification(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return
        if (pkgName == packageName) return // Ignore self

        if (!::repository.isInitialized) {
            repository = NotificationHistoryRepository.getInstance(applicationContext)
        }

        if (!repository.isCaptureEnabled.value) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract Title and Text
        var title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim()
            ?: ""

        var text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
            ?: ""
        var senderName = ""

        // Social Networks (WhatsApp, Telegram, Messenger, Signal): Support MessagingStyle bundles
        val messagesArray = extras.get("android.messages") as? Array<*>
        val capturedMessages = messagesArray.orEmpty().mapNotNull { messageItem ->
            val messageBundle = messageItem as? Bundle ?: return@mapNotNull null
            val messageText = messageBundle.getCharSequence("text")?.toString()?.trim()
                ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val messageSender = messageBundle.notificationSenderName().orEmpty()
            val messageTime = messageBundle.getLong("time", sbn.postTime).takeIf { it > 0L } ?: sbn.postTime
            Triple(messageSender, messageText, messageTime)
        }
        if (messagesArray != null && messagesArray.isNotEmpty()) {
            val lastBundle = messagesArray.lastOrNull() as? Bundle
            if (lastBundle != null) {
                val msgText = lastBundle.getCharSequence("text")?.toString()?.trim()
                val msgSender = lastBundle.notificationSenderName().orEmpty()
                if (!msgText.isNullOrEmpty()) {
                    text = msgText
                }
                if (!msgSender.isNullOrEmpty() && title.isEmpty()) {
                    title = msgSender
                }
                if (!msgSender.isNullOrEmpty()) senderName = msgSender
            }
        } else {
            // Also check android.textLines for inbox-style multi-message notifications
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (textLines != null && textLines.isNotEmpty() && text.isEmpty()) {
                text = textLines.lastOrNull()?.toString()?.trim() ?: ""
            }
        }

        // Skip completely empty system pings
        if (title.isEmpty() && text.isEmpty()) return
        if (senderName.isBlank()) senderName = title

        // Resolve App Name (e.g. WhatsApp, Telegram, Instagram)
        val pm = packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkgName.substringAfterLast('.')
        }

        // Extract Media (Photo / Video thumbnail / Image attachments)
        var mediaBitmap: Bitmap? = null
        var mediaType: String? = null

        // 1. Direct Picture Attachment (e.g., photo received in WhatsApp/Telegram)
        val extraPic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_PICTURE)
        }

        if (extraPic != null) {
            mediaBitmap = extraPic
            mediaType = "image"
        }

        // 2. Picture Icon (Android P+)
        if (mediaBitmap == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val iconExtra = extras.getParcelable(Notification.EXTRA_PICTURE_ICON) as? Icon
            if (iconExtra != null) {
                try {
                    val drawable = iconExtra.loadDrawable(this)
                    if (drawable is BitmapDrawable) {
                        mediaBitmap = drawable.bitmap
                        mediaType = "image"
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. Fallback: Large Icon if it represents a photo / media preview
        if (mediaBitmap == null) {
            val largeIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_LARGE_ICON)
            }
            if (largeIcon != null && (largeIcon.width > 96 || largeIcon.height > 96)) {
                // If it's a rich preview image rather than a tiny icon
                mediaBitmap = largeIcon
                mediaType = "image"
            }
        }

        // 4. Social Messaging Media Attachment URI in MessagingStyle bundles
        if (messagesArray != null) {
            for (msgItem in messagesArray) {
                val b = msgItem as? Bundle ?: continue
                val mimeType = b.getString("type")
                if (mimeType != null) {
                    if (mimeType.startsWith("image/")) mediaType = "image"
                    else if (mimeType.startsWith("audio/")) mediaType = "audio"
                    else if (mimeType.startsWith("video/")) mediaType = "video"
                }
                val uriStr = b.getString("uri")
                if (uriStr != null && mediaBitmap == null) {
                    try {
                        val uri = Uri.parse(uriStr)
                        contentResolver.openInputStream(uri)?.use { stream ->
                            mediaBitmap = BitmapFactory.decodeStream(stream)
                            if (mediaType == null) mediaType = "image"
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // 5. Detect audio / video message clues from text if mimeType not declared
        if (mediaType == null) {
            val combined = (title + " " + text).lowercase()
            when {
                combined.contains("audio") || combined.contains("vocal") || combined.contains("voice message") || combined.contains("mesaj vwa") -> {
                    mediaType = "audio"
                }
                combined.contains("video") || combined.contains("vidéo") -> {
                    mediaType = "video"
                }
                combined.contains("photo") || combined.contains("image") || combined.contains("picture") || combined.contains("foto") -> {
                    mediaType = "image"
                }
            }
        }

        val notificationChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notification.channelId else null
        val recentCapturedMessages = capturedMessages.takeLast(20)
        if (recentCapturedMessages.isNotEmpty()) {
            recentCapturedMessages.forEachIndexed { index, (messageSender, messageText, messageTime) ->
                repository.addNotification(
                    packageName = pkgName,
                    appName = appName,
                    title = title,
                    message = messageText,
                    channelId = notificationChannel,
                    senderName = messageSender.ifBlank { senderName },
                    timestampMillis = messageTime,
                    mediaBitmap = if (index == recentCapturedMessages.lastIndex) mediaBitmap else null,
                    mediaType = mediaType
                )
            }
        } else {
            repository.addNotification(
                packageName = pkgName,
                appName = appName,
                title = title,
                message = text,
                channelId = notificationChannel,
                senderName = senderName,
                timestampMillis = sbn.postTime,
                mediaBitmap = mediaBitmap,
                mediaType = mediaType
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun Bundle.notificationSenderName(): String? {
        getCharSequence("sender")?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return (getParcelable("sender_person") as? android.app.Person)?.name?.toString()?.trim()
        }
        return null
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Note: We intentionally do NOT delete the captured notification when removed from system bar.
        // This ensures that even if the sender deletes the message on WhatsApp, Telegram, etc.,
        // the captured history and saved media remain safely preserved in Hiris Controller!
    }
}
