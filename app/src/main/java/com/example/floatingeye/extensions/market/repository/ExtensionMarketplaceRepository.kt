package com.example.floatingeye.extensions.market.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.floatingeye.extensions.market.data.HirisExtensionsApi
import com.example.floatingeye.extensions.market.model.MarketplaceDownload
import com.example.floatingeye.extensions.market.model.MarketplaceExtension
import com.example.floatingeye.extensions.market.model.MarketplaceOperationResult
import com.example.floatingeye.extensions.market.model.MarketplaceUploadFile
import java.io.File

/** Coordinates marketplace requests and stores downloaded packages without executing them. */
class ExtensionMarketplaceRepository(
    context: Context,
    private val api: HirisExtensionsApi = HirisExtensionsApi()
) {
    private val appContext = context.applicationContext

    suspend fun list(token: String): List<MarketplaceExtension> = api.list(token)

    suspend fun upload(
        token: String,
        name: String,
        description: String,
        version: String,
        file: MarketplaceUploadFile
    ) = api.upload(token, name, description, version, file)

    suspend fun update(
        token: String,
        extensionId: String,
        name: String,
        description: String,
        version: String,
        file: MarketplaceUploadFile? = null
    ) = api.update(token, extensionId, name, description, version, file)

    suspend fun delete(token: String, extensionId: String) = api.delete(token, extensionId)

    suspend fun download(token: String, extensionId: String): MarketplaceOperationResult {
        val downloaded = api.download(token, extensionId)
        return storePackage(downloaded)
    }

    private fun storePackage(download: MarketplaceDownload): MarketplaceOperationResult {
        val filename = safeFilename(download.filename.ifBlank { download.extension.filename })
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            storeInPublicDownloads(download, filename)
        } else {
            storeInAppDownloads(download, filename)
        }
    }

    private fun storeInPublicDownloads(
        download: MarketplaceDownload,
        filename: String
    ): MarketplaceOperationResult {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, download.contentType)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/HIRIS Extensions")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = appContext.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create the download file.")
        try {
            resolver.openOutputStream(uri)?.use { output -> output.write(download.bytes) }
                ?: throw IllegalStateException("Could not write the downloaded extension.")
            val ready = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, ready, null, null)
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
        return MarketplaceOperationResult(
            savedLocation = "Downloads/HIRIS Extensions",
            filename = filename
        )
    }

    @Suppress("DEPRECATION")
    private fun storeInAppDownloads(
        download: MarketplaceDownload,
        filename: String
    ): MarketplaceOperationResult {
        val directory = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(appContext.filesDir, Environment.DIRECTORY_DOWNLOADS)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Could not create the download folder.")
        }
        File(directory, filename).writeBytes(download.bytes)
        return MarketplaceOperationResult(
            savedLocation = directory.absolutePath,
            filename = filename
        )
    }

    private fun safeFilename(filename: String): String =
        filename.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .ifBlank { "extension.bin" }
}
