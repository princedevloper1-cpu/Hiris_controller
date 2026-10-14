package com.example.floatingeye.auth

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HirisUser(
    val id: String,
    val username: String,
    val email: String,
    val createdAt: String
)

data class HirisAuthResult(val token: String, val user: HirisUser)

object HirisAuth {
    private const val BASE_URL = "https://hiris-controller-api.buisserethjohnpeter80.workers.dev"
    private const val PREFS = "hiris_auth"
    private const val KEY_TOKEN = "token"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun parseUser(json: JSONObject) = HirisUser(
        id = json.getString("id"),
        username = json.getString("username"),
        email = json.getString("email"),
        createdAt = json.getString("created_at")
    )

    private fun parseResult(body: String): HirisAuthResult {
        val json = JSONObject(body)
        return HirisAuthResult(json.getString("token"), parseUser(json.getJSONObject("user")))
    }

    private fun errorMessage(body: String, fallback: String): String = try {
        JSONObject(body).optString("error", fallback).ifBlank { fallback }
    } catch (_: Exception) {
        fallback
    }

    suspend fun signup(username: String, password: String): HirisAuthResult =
        authenticate("/api/signup", username, password, "Sign up failed.")

    suspend fun login(username: String, password: String): HirisAuthResult =
        authenticate("/api/login", username, password, "Invalid username or password.")

    private suspend fun authenticate(
        path: String,
        username: String,
        password: String,
        fallback: String
    ): HirisAuthResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("username", username.trim())
            .put("password", password)
            .toString()
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException(errorMessage(body, fallback))
            parseResult(body)
        }
    }

    suspend fun getMe(token: String): HirisUser? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/api/me")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string().orEmpty()
            parseUser(JSONObject(body).getJSONObject("user"))
        }
    }

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_TOKEN).apply()
    }
}
