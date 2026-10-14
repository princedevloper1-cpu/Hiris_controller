package com.example.floatingeye.notifications

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs

class NotificationHistoryRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mediaDir: File = File(context.filesDir, "notification_media").apply {
        if (!exists()) mkdirs()
    }

    private val _notifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
    val notifications: StateFlow<List<CapturedNotification>> = _notifications.asStateFlow()

    private val _isCaptureEnabled = MutableStateFlow(prefs.getBoolean(KEY_CAPTURE_ENABLED, false))
    val isCaptureEnabled: StateFlow<Boolean> = _isCaptureEnabled.asStateFlow()

    private val _selectedRetention = MutableStateFlow(
        RetentionPeriod.fromCode(prefs.getString(KEY_RETENTION, RetentionPeriod.HOURS_24.code))
    )
    val selectedRetention: StateFlow<RetentionPeriod> = _selectedRetention.asStateFlow()

    init {
        loadNotifications()
        purgeExpiredNotifications()
    }

    fun setCaptureEnabled(enabled: Boolean) {
        _isCaptureEnabled.value = enabled
        prefs.edit().putBoolean(KEY_CAPTURE_ENABLED, enabled).apply()
        if (enabled) HirisNotificationListenerService.captureActiveNotificationsIfConnected()
    }

    fun setRetentionPeriod(period: RetentionPeriod) {
        _selectedRetention.value = period
        prefs.edit().putString(KEY_RETENTION, period.code).apply()
        purgeExpiredNotifications()
    }

    @Synchronized
    fun addNotification(
        packageName: String,
        appName: String,
        title: String,
        message: String,
        channelId: String? = null,
        senderName: String? = null,
        timestampMillis: Long? = null,
        mediaBitmap: Bitmap? = null,
        mediaType: String? = null
    ) {
        // Prevent duplicate spam within 3 seconds for identical package + title + message
        val now = System.currentTimeMillis()
        val capturedAt = timestampMillis?.takeIf { it > 0L } ?: now
        val effectiveSenderName = senderName?.takeIf { it.isNotBlank() } ?: title.takeIf { it.isNotBlank() }
        val currentList = _notifications.value
        val recentMatch = currentList.firstOrNull {
            it.packageName == packageName &&
                    it.title == title &&
                    it.message == message &&
                    (it.senderName?.takeIf { name -> name.isNotBlank() } ?: it.title.takeIf { name -> name.isNotBlank() }) == effectiveSenderName &&
                    abs(it.timestamp - capturedAt) < 3000L
        }
        if (recentMatch != null) return

        var savedMediaPath: String? = null
        if (mediaBitmap != null) {
            savedMediaPath = saveMediaBitmap(mediaBitmap)
        }

        val newItem = CapturedNotification(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            appName = appName,
            title = title,
            message = message,
            timestamp = capturedAt,
            mediaPath = savedMediaPath,
            mediaType = if (savedMediaPath != null) (mediaType ?: "image") else null,
            channelId = channelId,
            senderName = effectiveSenderName
        )

        // Add at the beginning (newest first)
        val updated = listOf(newItem) + currentList
        _notifications.value = updated
        persistNotifications(updated)

        // Trigger retention cleanup
        purgeExpiredNotifications()
    }

    private fun saveMediaBitmap(bitmap: Bitmap): String? {
        return try {
            val file = File(mediaDir, "media_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteNotification(id: String) {
        val current = _notifications.value
        val target = current.firstOrNull { it.id == id }
        target?.mediaPath?.let { path ->
            try {
                File(path).delete()
            } catch (_: Exception) {}
        }
        val updated = current.filterNot { it.id == id }
        _notifications.value = updated
        persistNotifications(updated)
    }

    fun clearAll() {
        // Delete all local media files
        scope.launch {
            try {
                mediaDir.listFiles()?.forEach { it.delete() }
            } catch (_: Exception) {}
        }
        _notifications.value = emptyList()
        prefs.edit().remove(KEY_NOTIFICATIONS_JSON).apply()
    }

    /**
     * Purges notifications that exceed the user's selected retention period (8h, 24h, 1 semaine).
     */
    fun purgeExpiredNotifications() {
        val retention = _selectedRetention.value
        if (retention == RetentionPeriod.NEVER) return

        val thresholdMs = retention.durationHours * 60 * 60 * 1000L
        val cutoff = System.currentTimeMillis() - thresholdMs
        val current = _notifications.value

        val (kept, expired) = current.partition { it.timestamp >= cutoff }

        if (expired.isNotEmpty()) {
            scope.launch {
                expired.forEach { item ->
                    item.mediaPath?.let { path ->
                        try {
                            File(path).delete()
                        } catch (_: Exception) {}
                    }
                }
            }
            _notifications.value = kept
            persistNotifications(kept)
        }
    }

    private fun loadNotifications() {
        val jsonStr = prefs.getString(KEY_NOTIFICATIONS_JSON, null)
        if (jsonStr.isNullOrEmpty()) {
            _notifications.value = emptyList()
            return
        }

        try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<CapturedNotification>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val mediaPath = obj.optString("mediaPath").takeIf { it.isNotEmpty() }
                // Verify media file still exists if referenced
                val validMediaPath = if (mediaPath != null && File(mediaPath).exists()) mediaPath else null

                list.add(
                    CapturedNotification(
                        id = obj.getString("id"),
                        packageName = obj.getString("packageName"),
                        appName = obj.optString("appName", "Unknown App"),
                        title = obj.optString("title", ""),
                        message = obj.optString("message", ""),
                        timestamp = obj.getLong("timestamp"),
                        mediaPath = validMediaPath,
                        mediaType = obj.optString("mediaType").takeIf { it.isNotEmpty() },
                        channelId = obj.optString("channelId").takeIf { it.isNotEmpty() },
                        senderName = obj.optString("senderName").takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
            _notifications.value = list
        } catch (e: Exception) {
            e.printStackTrace()
            _notifications.value = emptyList()
        }
    }

    private fun persistNotifications(list: List<CapturedNotification>) {
        scope.launch {
            try {
                val jsonArray = JSONArray()
                // Keep max 800 recent notifications for high performance
                val limited = list.take(800)
                for (item in limited) {
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("packageName", item.packageName)
                        put("appName", item.appName)
                        put("title", item.title)
                        put("message", item.message)
                        put("timestamp", item.timestamp)
                        put("mediaPath", item.mediaPath ?: "")
                        put("mediaType", item.mediaType ?: "")
                        put("channelId", item.channelId ?: "")
                        put("senderName", item.senderName ?: "")
                    }
                    jsonArray.put(obj)
                }
                prefs.edit().putString(KEY_NOTIFICATIONS_JSON, jsonArray.toString()).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "hiris_notification_history"
        private const val KEY_NOTIFICATIONS_JSON = "saved_notifications_json"
        private const val KEY_CAPTURE_ENABLED = "notification_capture_enabled"
        private const val KEY_RETENTION = "notification_retention_period"

        @Volatile
        private var instance: NotificationHistoryRepository? = null

        fun getInstance(context: Context): NotificationHistoryRepository {
            return instance ?: synchronized(this) {
                instance ?: NotificationHistoryRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
