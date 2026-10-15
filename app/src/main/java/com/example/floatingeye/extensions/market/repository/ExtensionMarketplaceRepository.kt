package com.example.floatingeye.extensions.market.repository

import android.content.Context
import com.example.floatingeye.extensions.market.data.HirisExtensionsApi
import com.example.floatingeye.extensions.market.model.LocalExtensionPackage
import com.example.floatingeye.extensions.market.model.MarketplaceDownload
import com.example.floatingeye.extensions.market.model.MarketplaceExtension
import com.example.floatingeye.extensions.market.model.MarketplaceOperationResult
import com.example.floatingeye.extensions.market.model.MarketplaceUploadFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Handles the online market and a persistent, app-private library of downloaded packages. */
class ExtensionMarketplaceRepository private constructor(
    context: Context,
    private val api: HirisExtensionsApi = HirisExtensionsApi()
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val packageDirectory = File(appContext.filesDir, "hiris_extensions")
    private val _installedPackages = MutableStateFlow(loadInstalledPackages())
    val installedPackages: StateFlow<List<LocalExtensionPackage>> = _installedPackages.asStateFlow()

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

    fun removeLocal(extensionId: String) {
        val removed = _installedPackages.value.firstOrNull { it.extension.id == extensionId } ?: return
        File(packageDirectory, removed.storedFilename).delete()
        val updated = _installedPackages.value.filterNot { it.extension.id == extensionId }
        persist(updated)
        _installedPackages.value = updated
    }

    private fun storePackage(download: MarketplaceDownload): MarketplaceOperationResult {
        if (!packageDirectory.exists() && !packageDirectory.mkdirs()) {
            throw IllegalStateException("Could not create HIRIS's local extensions folder.")
        }
        val filename = safeFilename(download.filename.ifBlank { download.extension.filename })
        val storedFilename = "${safeFilename(download.extension.id)}__$filename"
        val target = File(packageDirectory, storedFilename)
        val temporary = File(packageDirectory, "$storedFilename.tmp")
        try {
            temporary.writeBytes(download.bytes)
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }

        val updated = _installedPackages.value
            .filterNot { it.extension.id == download.extension.id } +
            LocalExtensionPackage(download.extension, storedFilename)
        persist(updated)
        _installedPackages.value = updated
        return MarketplaceOperationResult(
            savedLocation = "HIRIS Controller > My extensions",
            filename = filename
        )
    }

    private fun loadInstalledPackages(): List<LocalExtensionPackage> {
        val array = runCatching { JSONArray(preferences.getString(KEY_PACKAGES, "[]")) }.getOrNull()
            ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val entry = runCatching { array.getJSONObject(index) }.getOrNull() ?: continue
                val storedFilename = entry.optString("storedFilename")
                if (storedFilename.isBlank() || !File(packageDirectory, storedFilename).isFile) continue
                val metadata = runCatching { entry.getJSONObject("extension") }.getOrNull() ?: continue
                val extension = runCatching { parseExtension(metadata) }.getOrNull() ?: continue
                add(LocalExtensionPackage(extension, storedFilename))
            }
        }
    }

    private fun persist(packages: List<LocalExtensionPackage>) {
        val array = JSONArray()
        packages.forEach { local ->
            array.put(
                JSONObject()
                    .put("storedFilename", local.storedFilename)
                    .put("extension", local.extension.toJson())
            )
        }
        preferences.edit().putString(KEY_PACKAGES, array.toString()).apply()
    }

    private fun MarketplaceExtension.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("description", description)
        .put("version", version)
        .put("filename", filename)
        .put("contentType", contentType)
        .put("size", size)
        .put("downloads", downloads)
        .put("author", author)
        .put("created_at", createdAt)
        .put("updated_at", updatedAt)

    private fun parseExtension(json: JSONObject) = MarketplaceExtension(
        id = json.getString("id"),
        name = json.getString("name"),
        description = json.optString("description", ""),
        version = json.getString("version"),
        filename = json.getString("filename"),
        contentType = json.optString("contentType", "application/octet-stream"),
        size = json.optLong("size", 0),
        downloads = json.optInt("downloads", 0),
        author = json.optString("author", ""),
        createdAt = json.optString("created_at", ""),
        updatedAt = json.optString("updated_at", "")
    )

    private fun safeFilename(filename: String): String =
        filename.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .ifBlank { "extension.bin" }

    companion object {
        private const val PREFERENCES = "hiris_extension_marketplace"
        private const val KEY_PACKAGES = "downloaded_packages"

        @Volatile
        private var instance: ExtensionMarketplaceRepository? = null

        fun getInstance(context: Context): ExtensionMarketplaceRepository =
            instance ?: synchronized(this) {
                instance ?: ExtensionMarketplaceRepository(context.applicationContext).also { instance = it }
            }
    }
}
