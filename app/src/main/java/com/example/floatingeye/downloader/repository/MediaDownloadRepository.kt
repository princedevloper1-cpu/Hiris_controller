package com.example.floatingeye.downloader.repository

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.example.floatingeye.downloader.model.DownloadStatus
import com.example.floatingeye.downloader.model.MediaDownloadItem
import com.example.floatingeye.downloader.model.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class MediaDownloadRepository private constructor(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _downloads = MutableStateFlow<List<MediaDownloadItem>>(loadDownloads())
    val downloads: StateFlow<List<MediaDownloadItem>> = _downloads.asStateFlow()

    private val onCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: return
            if (id != -1L) {
                checkDownloadStatus(id)
            }
        }
    }

    init {
        try {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(onCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(onCompleteReceiver, filter)
            }
        } catch (_: Exception) {}

        // Start progress polling loop for any ongoing downloads
        startProgressPoller()
    }

    private fun startProgressPoller() {
        repositoryScope.launch {
            while (true) {
                delay(1200)
                val current = _downloads.value
                val hasActive = current.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
                if (hasActive && downloadManager != null) {
                    var changed = false
                    val updated = current.map { item ->
                        if (item.downloadManagerId > 0 && (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PENDING)) {
                            val query = DownloadManager.Query().setFilterById(item.downloadManagerId)
                            val cursor = downloadManager.query(query)
                            var newBytes = item.downloadedBytes
                            var newTotal = item.totalBytes
                            var newStatus = item.status

                            if (cursor != null && cursor.moveToFirst()) {
                                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                                if (bytesDownloadedIndex >= 0) newBytes = cursor.getLong(bytesDownloadedIndex)
                                if (bytesTotalIndex >= 0) newTotal = cursor.getLong(bytesTotalIndex)
                                if (statusIndex >= 0) {
                                    val status = cursor.getInt(statusIndex)
                                    newStatus = when (status) {
                                        DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                                        DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                                        DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                                        else -> DownloadStatus.PENDING
                                    }
                                }
                                cursor.close()
                                if (newBytes != item.downloadedBytes || newStatus != item.status || newTotal != item.totalBytes) {
                                    changed = true
                                    item.copy(
                                        downloadedBytes = newBytes,
                                        totalBytes = newTotal,
                                        status = newStatus
                                    )
                                } else item
                            } else {
                                cursor?.close()
                                item
                            }
                        } else item
                    }
                    if (changed) {
                        saveDownloads(updated)
                    }
                }
            }
        }
    }

    private fun checkDownloadStatus(dmId: Long) {
        val dm = downloadManager ?: return
        val query = DownloadManager.Query().setFilterById(dmId)
        val cursor = dm.query(query) ?: return
        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex >= 0) {
                val status = cursor.getInt(statusIndex)
                val newStatus = if (status == DownloadManager.STATUS_SUCCESSFUL) DownloadStatus.COMPLETED else DownloadStatus.FAILED
                val current = _downloads.value
                val updated = current.map {
                    if (it.downloadManagerId == dmId) it.copy(status = newStatus) else it
                }
                saveDownloads(updated)
            }
        }
        cursor.close()
    }

    private fun loadDownloads(): List<MediaDownloadItem> {
        val jsonStr = prefs.getString(KEY_DOWNLOADS_JSON, null) ?: return defaultSampleDownloads()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<MediaDownloadItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MediaDownloadItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        sourceUrl = obj.getString("sourceUrl"),
                        mediaType = try {
                            MediaType.valueOf(obj.getString("mediaType"))
                        } catch (_: Exception) {
                            MediaType.VIDEO
                        },
                        qualityLabel = obj.optString("qualityLabel", "Auto/Direct"),
                        destinationPath = obj.optString("destinationPath", ""),
                        totalBytes = obj.optLong("totalBytes", -1L),
                        downloadedBytes = obj.optLong("downloadedBytes", 0L),
                        status = try {
                            DownloadStatus.valueOf(obj.getString("status"))
                        } catch (_: Exception) {
                            DownloadStatus.COMPLETED
                        },
                        downloadManagerId = obj.optLong("downloadManagerId", -1L),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.createdAtMillis }
        } catch (_: Exception) {
            defaultSampleDownloads()
        }
    }

    private fun defaultSampleDownloads(): List<MediaDownloadItem> {
        return listOf(
            MediaDownloadItem(
                id = "demo_1",
                title = "Cyberpunk Neon Theme Loop",
                sourceUrl = "https://assets.mixkit.co/videos/preview/mixkit-futuristic-city-with-flying-cars-and-skyscrapers-41551-large.mp4",
                mediaType = MediaType.VIDEO,
                qualityLabel = "1080p MP4",
                totalBytes = 8_420_000L,
                downloadedBytes = 8_420_000L,
                status = DownloadStatus.COMPLETED
            ),
            MediaDownloadItem(
                id = "demo_2",
                title = "HIRIS Synthwave Beat",
                sourceUrl = "https://assets.mixkit.co/music/preview/mixkit-tech-house-vibes-130.mp3",
                mediaType = MediaType.AUDIO,
                qualityLabel = "320kbps MP3",
                totalBytes = 4_100_000L,
                downloadedBytes = 4_100_000L,
                status = DownloadStatus.COMPLETED
            )
        )
    }

    private fun saveDownloads(list: List<MediaDownloadItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("sourceUrl", item.sourceUrl)
                put("mediaType", item.mediaType.name)
                put("qualityLabel", item.qualityLabel)
                put("destinationPath", item.destinationPath)
                put("totalBytes", item.totalBytes)
                put("downloadedBytes", item.downloadedBytes)
                put("status", item.status.name)
                put("downloadManagerId", item.downloadManagerId)
                put("createdAtMillis", item.createdAtMillis)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_DOWNLOADS_JSON, array.toString()).apply()
        _downloads.value = list.sortedByDescending { it.createdAtMillis }
    }

    fun startDownload(
        url: String,
        customTitle: String? = null,
        type: MediaType = MediaType.VIDEO,
        quality: String = "Auto/Direct"
    ): Result<MediaDownloadItem> {
        val trimmedUrl = url.trim()
        if (!trimmedUrl.startsWith("http://", ignoreCase = true) && !trimmedUrl.startsWith("https://", ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Lyen an dwe kòmanse ak http:// oswa https://"))
        }

        val parsedUri = try {
            Uri.parse(trimmedUrl)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Lyen an pa valab: ${e.message}"))
        }

        val lastPathSegment = parsedUri.lastPathSegment ?: ""
        val extension = when {
            type == MediaType.AUDIO && !lastPathSegment.endsWith(".mp3", ignoreCase = true) -> ".mp3"
            type == MediaType.VIDEO && !lastPathSegment.endsWith(".mp4", ignoreCase = true) -> ".mp4"
            else -> ""
        }

        val fileName = if (!customTitle.isNullOrBlank()) {
            "${customTitle.trim().replace("[^a-zA-Z0-9_.-]".toRegex(), "_")}$extension"
        } else if (lastPathSegment.isNotBlank()) {
            "$lastPathSegment$extension"
        } else {
            "media_${System.currentTimeMillis()}$extension"
        }

        val subDir = if (type == MediaType.AUDIO) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES

        val dm = downloadManager
        var dmId = -1L
        if (dm != null) {
            try {
                val request = DownloadManager.Request(parsedUri).apply {
                    setTitle(if (!customTitle.isNullOrBlank()) customTitle else fileName)
                    setDescription("Telechaje ak HIRIS Media Vault")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(subDir, fileName)
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }
                dmId = dm.enqueue(request)
            } catch (e: Exception) {
                // If DownloadManager public dir fails due to platform restrictions, fallback to private cache
                try {
                    val request = DownloadManager.Request(parsedUri).apply {
                        setTitle(if (!customTitle.isNullOrBlank()) customTitle else fileName)
                        setDescription("Telechaje ak HIRIS Media Vault")
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalFilesDir(context, subDir, fileName)
                    }
                    dmId = dm.enqueue(request)
                } catch (fallbackError: Exception) {
                    return Result.failure(fallbackError)
                }
            }
        }

        val newItem = MediaDownloadItem(
            id = UUID.randomUUID().toString(),
            title = if (!customTitle.isNullOrBlank()) customTitle else fileName,
            sourceUrl = trimmedUrl,
            mediaType = type,
            qualityLabel = quality,
            destinationPath = "$subDir/$fileName",
            status = DownloadStatus.DOWNLOADING,
            downloadManagerId = dmId,
            createdAtMillis = System.currentTimeMillis()
        )

        val updated = listOf(newItem) + _downloads.value
        saveDownloads(updated)
        return Result.success(newItem)
    }

    fun removeDownload(id: String) {
        val item = _downloads.value.firstOrNull { it.id == id }
        if (item != null && item.downloadManagerId > 0 && downloadManager != null) {
            try {
                downloadManager.remove(item.downloadManagerId)
            } catch (_: Exception) {}
        }
        val updated = _downloads.value.filter { it.id != id }
        saveDownloads(updated)
    }

    companion object {
        private const val PREFS_NAME = "hiris_downloader_prefs"
        private const val KEY_DOWNLOADS_JSON = "key_downloads_json"

        @Volatile
        private var instance: MediaDownloadRepository? = null

        fun getInstance(context: Context): MediaDownloadRepository {
            return instance ?: synchronized(this) {
                instance ?: MediaDownloadRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
