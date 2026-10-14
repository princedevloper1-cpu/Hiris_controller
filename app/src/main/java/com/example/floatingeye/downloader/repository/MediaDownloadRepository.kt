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
                            var newErrorMessage = item.errorMessage

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
                                    val mediaTypeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
                                    val contentType = if (mediaTypeIndex >= 0) cursor.getString(mediaTypeIndex).orEmpty() else ""
                                    if (status == DownloadManager.STATUS_SUCCESSFUL && !isExpectedMediaType(contentType, item.mediaType)) {
                                        newStatus = DownloadStatus.FAILED
                                        newErrorMessage = "Lyen an pa t bay yon fichye ${if (item.mediaType == MediaType.AUDIO) "odyo" else "videyo"} dirèk. Kole lyen MP3/MP4 fichye a, pa paj sit la."
                                        downloadManager.remove(item.downloadManagerId)
                                    } else if (status == DownloadManager.STATUS_FAILED) {
                                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                        val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex) else 0
                                        newErrorMessage = downloadFailureMessage(reason)
                                    } else {
                                        newErrorMessage = ""
                                    }
                                }
                                cursor.close()
                                if (newBytes != item.downloadedBytes || newStatus != item.status || newTotal != item.totalBytes || newErrorMessage != item.errorMessage) {
                                    changed = true
                                    item.copy(
                                        downloadedBytes = newBytes,
                                        totalBytes = newTotal,
                                        status = newStatus,
                                        errorMessage = newErrorMessage
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
                val current = _downloads.value
                val item = current.firstOrNull { it.downloadManagerId == dmId }
                if (item != null) {
                    val status = cursor.getInt(statusIndex)
                    val mediaTypeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
                    val contentType = if (mediaTypeIndex >= 0) cursor.getString(mediaTypeIndex).orEmpty() else ""
                    val badContent = status == DownloadManager.STATUS_SUCCESSFUL && !isExpectedMediaType(contentType, item.mediaType)
                    val newStatus = when {
                        badContent -> DownloadStatus.FAILED
                        status == DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                        status == DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                        status == DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                        else -> DownloadStatus.PENDING
                    }
                    val errorMessage = when {
                        badContent -> "Lyen an pa t bay yon fichye ${if (item.mediaType == MediaType.AUDIO) "odyo" else "videyo"} dirèk. Kole lyen MP3/MP4 fichye a, pa paj sit la."
                        status == DownloadManager.STATUS_FAILED -> {
                            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            downloadFailureMessage(if (reasonIndex >= 0) cursor.getInt(reasonIndex) else 0)
                        }
                        else -> ""
                    }
                    if (badContent) dm.remove(dmId)
                    val updated = current.map {
                        if (it.downloadManagerId == dmId) it.copy(status = newStatus, errorMessage = errorMessage) else it
                    }
                    saveDownloads(updated)
                }
            }
        }
        cursor.close()
    }

    private fun isExpectedMediaType(contentType: String, type: MediaType): Boolean {
        if (contentType.isBlank() || contentType == "application/octet-stream") return true
        if (contentType.startsWith("text/", ignoreCase = true) || contentType.contains("html", ignoreCase = true)) return false
        return when (type) {
            MediaType.AUDIO -> contentType.startsWith("audio/", ignoreCase = true) || contentType.equals("application/mp3", ignoreCase = true)
            MediaType.VIDEO -> contentType.startsWith("video/", ignoreCase = true) || contentType.equals("application/mp4", ignoreCase = true)
        }
    }

    private fun downloadFailureMessage(reason: Int): String = when (reason) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Pa gen ase espas nan telefòn nan."
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Kat memwa a pa disponib."
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE,
        DownloadManager.ERROR_HTTP_DATA_ERROR,
        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Sèvè a pa t bay fichye a. Verifye lyen dirèk la oswa eseye ankò pita."
        DownloadManager.ERROR_CANNOT_RESUME -> "Telechajman an te koupe; rekòmanse li."
        else -> "Telechajman an echwe. Verifye koneksyon an ak lyen dirèk fichye a."
    }

    private fun loadDownloads(): List<MediaDownloadItem> {
        val jsonStr = prefs.getString(KEY_DOWNLOADS_JSON, null) ?: return emptyList()
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
                        errorMessage = obj.optString("errorMessage", ""),
                        downloadManagerId = obj.optLong("downloadManagerId", -1L),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.createdAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
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
                put("errorMessage", item.errorMessage)
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
        if (parsedUri.host.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Lyen an pa gen adrès sit entènèt ki valab."))
        }

        val dm = downloadManager
            ?: return Result.failure(IllegalStateException("Sèvis Download Manager Android lan pa disponib sou telefòn sa a."))

        val pathName = parsedUri.lastPathSegment.orEmpty().substringBefore('?').substringBefore('#')
        val pathExtension = pathName.substringAfterLast('.', "").lowercase()
        val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "ogg", "opus", "flac")
        val videoExtensions = setOf("mp4", "m4v", "mov", "webm", "mkv", "3gp")
        if (pathExtension == "m3u8" || pathExtension == "mpd") {
            return Result.failure(IllegalArgumentException("Lyen sa a se yon playlist streaming. Downloader la bezwen yon lyen dirèk pou fichye MP3 oswa MP4."))
        }
        if (type == MediaType.AUDIO && pathExtension in videoExtensions) {
            return Result.failure(IllegalArgumentException("Lyen sa a mennen nan yon videyo. App la pa konvèti videyo pou fè MP3; chwazi Videyo oswa bay yon lyen MP3 dirèk."))
        }
        if (type == MediaType.VIDEO && pathExtension in audioExtensions) {
            return Result.failure(IllegalArgumentException("Lyen sa a mennen nan yon odyo. Chwazi Odyo pou telechaje fichye MP3 la."))
        }
        if (type == MediaType.AUDIO && pathExtension in audioExtensions && pathExtension != "mp3") {
            return Result.failure(IllegalArgumentException("Lyen sa a se yon fichye .$pathExtension. App la pa konvèti fichye odyo an MP3; bay yon lyen MP3 dirèk."))
        }
        if (type == MediaType.VIDEO && pathExtension in videoExtensions && pathExtension != "mp4") {
            return Result.failure(IllegalArgumentException("Lyen sa a se yon fichye .$pathExtension. App la pa konvèti videyo an MP4; bay yon lyen MP4 dirèk."))
        }

        val expectedExtension = if (type == MediaType.AUDIO) ".mp3" else ".mp4"
        val rawName = if (!customTitle.isNullOrBlank()) customTitle.trim() else pathName.substringBeforeLast('.', pathName)
        val safeName = rawName
            .replace(Regex("[^\\p{L}\\p{N}._ -]"), "_")
            .trim(' ', '.')
            .ifBlank { "media_${System.currentTimeMillis()}" }
        val baseName = if (safeName.endsWith(expectedExtension, ignoreCase = true)) {
            safeName.dropLast(expectedExtension.length)
        } else {
            safeName
        }
        val fileName = "$baseName$expectedExtension"

        val subDir = if (type == MediaType.AUDIO) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES

        val dmId = try {
            val request = DownloadManager.Request(parsedUri).apply {
                setTitle(baseName)
                setDescription("Telechaje dirèk ak HIRIS Media Vault")
                setMimeType(if (type == MediaType.AUDIO) "audio/mpeg" else "video/mp4")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(subDir, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            dm.enqueue(request)
        } catch (e: Exception) {
            return Result.failure(IllegalStateException("Android pa t ka kòmanse telechajman an: ${e.localizedMessage ?: "erè enkoni"}", e))
        }
        if (dmId <= 0L) {
            return Result.failure(IllegalStateException("Android pa t aksepte demann telechajman an."))
        }

        val newItem = MediaDownloadItem(
            id = UUID.randomUUID().toString(),
            title = if (!customTitle.isNullOrBlank()) baseName else fileName,
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

    fun getDownloadedFileUri(item: MediaDownloadItem): Uri? {
        if (item.status != DownloadStatus.COMPLETED || item.downloadManagerId <= 0L) return null
        return try {
            downloadManager?.getUriForDownloadedFile(item.downloadManagerId)
        } catch (_: Exception) {
            null
        }
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
