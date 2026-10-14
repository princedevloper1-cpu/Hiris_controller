package com.example.floatingeye.downloader.model

enum class MediaType {
    VIDEO,
    AUDIO
}

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class MediaDownloadItem(
    val id: String,
    val title: String,
    val sourceUrl: String,
    val mediaType: MediaType,
    val qualityLabel: String = "Auto/Direct",
    val destinationPath: String = "",
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val errorMessage: String = "",
    val downloadManagerId: Long = -1L,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val progressPercent: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else if (status == DownloadStatus.COMPLETED) {
            1f
        } else {
            0f
        }
}
