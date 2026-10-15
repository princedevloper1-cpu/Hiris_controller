package com.example.floatingeye.extensions.market.data

import android.net.Uri
import com.example.floatingeye.auth.HirisApiConfig
import com.example.floatingeye.extensions.market.model.MarketplaceDownload
import com.example.floatingeye.extensions.market.model.MarketplaceExtension
import com.example.floatingeye.extensions.market.model.MarketplaceUploadFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** HTTP client for the authenticated extensions marketplace endpoints. */
class HirisExtensionsApi(
    private val client: OkHttpClient = defaultClient
) {
    suspend fun list(token: String): List<MarketplaceExtension> = withContext(Dispatchers.IO) {
        val request = authenticatedRequest(token, "api/extensions").get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            ensureSuccessful(response.code, body, "Failed to load extensions.")
            val extensions = JSONObject(body).getJSONArray("extensions")
            List(extensions.length()) { index -> parseExtension(extensions.getJSONObject(index)) }
        }
    }

    suspend fun get(token: String, extensionId: String): MarketplaceExtension = withContext(Dispatchers.IO) {
        val request = authenticatedExtensionRequest(token, extensionId).get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            ensureSuccessful(response.code, body, "Extension not found.")
            parseExtension(JSONObject(body).getJSONObject("extension"))
        }
    }

    suspend fun download(token: String, extensionId: String): MarketplaceDownload = withContext(Dispatchers.IO) {
        val request = authenticatedExtensionRequest(token, extensionId, "download").get().build()
        client.newCall(request).execute().use { response ->
            val bytes = response.body?.bytes() ?: ByteArray(0)
            if (!response.isSuccessful) {
                ensureSuccessful(response.code, bytes.toString(Charsets.UTF_8), "Download failed.")
            }
            if (bytes.size > MAX_FILE_BYTES) {
                throw IllegalStateException("The downloaded extension exceeds the 5 MB limit.")
            }
            // Downloads return the raw file. Fetch metadata separately as required by the API.
            val metadata = get(token, extensionId)
            val disposition = response.header("Content-Disposition")
            val filename = disposition
                ?.substringAfter("filename=", metadata.filename)
                ?.trim('"')
                ?.substringAfterLast('/')
                ?.substringAfterLast('\\')
                ?.takeIf(String::isNotBlank)
                ?: metadata.filename
            MarketplaceDownload(
                extension = metadata,
                bytes = bytes,
                filename = filename,
                contentType = response.header("Content-Type") ?: metadata.contentType
            )
        }
    }

    suspend fun upload(
        token: String,
        name: String,
        description: String,
        version: String,
        file: MarketplaceUploadFile
    ): MarketplaceExtension = withContext(Dispatchers.IO) {
        checkFileSize(file.bytes)
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("name", name.trim())
            .addFormDataPart("description", description.trim())
            .addFormDataPart("version", version.trim())
            .addFormDataPart(
                "file",
                safeFilename(file.filename),
                file.bytes.toRequestBody(file.contentType.toMediaTypeOrNull())
            )
            .build()
        val request = authenticatedRequest(token, "api/extensions").post(multipart).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            ensureSuccessful(response.code, body, "Upload failed.")
            parseExtension(JSONObject(body).getJSONObject("extension"))
        }
    }

    suspend fun update(
        token: String,
        extensionId: String,
        name: String,
        description: String,
        version: String,
        replacementFile: MarketplaceUploadFile? = null
    ): MarketplaceExtension = withContext(Dispatchers.IO) {
        replacementFile?.let { checkFileSize(it.bytes) }
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("name", name.trim())
            .addFormDataPart("description", description.trim())
            .addFormDataPart("version", version.trim())
        replacementFile?.let { file ->
            multipart.addFormDataPart(
                "file",
                safeFilename(file.filename),
                file.bytes.toRequestBody(file.contentType.toMediaTypeOrNull())
            )
        }
        val request = authenticatedExtensionRequest(token, extensionId)
            .put(multipart.build())
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            ensureSuccessful(response.code, body, "Update failed.")
            parseExtension(JSONObject(body).getJSONObject("extension"))
        }
    }

    suspend fun delete(token: String, extensionId: String): Unit = withContext(Dispatchers.IO) {
        val request = authenticatedExtensionRequest(token, extensionId).delete().build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            ensureSuccessful(response.code, body, "Delete failed.")
        }
    }

    private fun authenticatedRequest(token: String, vararg path: String): Request.Builder {
        val url = HirisApiConfig.BASE_URL.trimEnd('/') + "/" +
            path.joinToString("/") { Uri.encode(it) }
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
    }

    private fun authenticatedExtensionRequest(
        token: String,
        extensionId: String,
        vararg suffix: String
    ): Request.Builder = authenticatedRequest(token, "api", "extensions", extensionId, *suffix)

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

    private fun ensureSuccessful(code: Int, body: String, fallback: String) {
        if (code in 200..299) return
        val serverMessage = try {
            JSONObject(body).optString("error", fallback).ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
        val message = when (code) {
            401 -> "Please log in to access the extension market."
            403 -> "Admin access is required for this action."
            else -> serverMessage
        }
        throw IllegalStateException(message)
    }

    private fun checkFileSize(bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Choose an extension file first." }
        require(bytes.size <= MAX_FILE_BYTES) { "Extension files must be 5 MB or smaller." }
    }

    private fun safeFilename(filename: String): String =
        filename.substringAfterLast('/').substringAfterLast('\\').replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .ifBlank { "extension.bin" }

    companion object {
        const val MAX_FILE_BYTES = 5 * 1024 * 1024
        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
