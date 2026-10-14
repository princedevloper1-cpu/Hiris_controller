package com.example.floatingeye.gestures

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class GestureRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _gestures = MutableStateFlow<List<GestureAppBinding>>(emptyList())
    val gestures: StateFlow<List<GestureAppBinding>> = _gestures.asStateFlow()

    init {
        loadGestures()
    }

    private fun loadGestures() {
        val jsonString = prefs.getString(KEY_GESTURES, null)
        if (jsonString.isNullOrEmpty()) {
            _gestures.value = emptyList()
            return
        }

        try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<GestureAppBinding>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val normPointsArray = obj.getJSONArray("normalizedPoints")
                val normPoints = mutableListOf<GesturePoint>()
                for (j in 0 until normPointsArray.length()) {
                    val p = normPointsArray.getJSONObject(j)
                    normPoints.add(
                        GesturePoint(
                            x = p.getDouble("x").toFloat(),
                            y = p.getDouble("y").toFloat()
                        )
                    )
                }

                val rawPointsArray = obj.optJSONArray("rawPoints")
                val rawPoints = mutableListOf<GesturePoint>()
                if (rawPointsArray != null) {
                    for (j in 0 until rawPointsArray.length()) {
                        val p = rawPointsArray.getJSONObject(j)
                        rawPoints.add(
                            GesturePoint(
                                x = p.getDouble("x").toFloat(),
                                y = p.getDouble("y").toFloat()
                            )
                        )
                    }
                }

                list.add(
                    GestureAppBinding(
                        id = obj.getString("id"),
                        packageName = obj.getString("packageName"),
                        appName = obj.getString("appName"),
                        normalizedPoints = normPoints,
                        rawPoints = rawPoints,
                        isEnabled = obj.optBoolean("isEnabled", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            _gestures.value = list
        } catch (e: Exception) {
            _gestures.value = emptyList()
        }
    }

    private fun persist(list: List<GestureAppBinding>) {
        val jsonArray = JSONArray()

        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("packageName", item.packageName)
                put("appName", item.appName)
                put("isEnabled", item.isEnabled)
                put("createdAt", item.createdAt)

                val normArr = JSONArray()
                for (p in item.normalizedPoints) {
                    normArr.put(JSONObject().apply {
                        put("x", p.x.toDouble())
                        put("y", p.y.toDouble())
                    })
                }
                put("normalizedPoints", normArr)

                val rawArr = JSONArray()
                for (p in item.rawPoints) {
                    rawArr.put(JSONObject().apply {
                        put("x", p.x.toDouble())
                        put("y", p.y.toDouble())
                    })
                }
                put("rawPoints", rawArr)
            }
            jsonArray.put(obj)
        }

        prefs.edit().putString(KEY_GESTURES, jsonArray.toString()).apply()
        loadGestures()
    }

    fun saveGesture(binding: GestureAppBinding) {
        val current = _gestures.value.toMutableList()
        val index = current.indexOfFirst { it.id == binding.id }
        if (index >= 0) {
            current[index] = binding
        } else {
            current.add(binding)
        }
        persist(current)
    }

    fun deleteGesture(id: String) {
        val current = _gestures.value.filter { it.id != id }
        persist(current)
    }

    fun toggleEnabled(id: String, isEnabled: Boolean) {
        val current = _gestures.value.map {
            if (it.id == id) it.copy(isEnabled = isEnabled) else it
        }
        persist(current)
    }

    /**
     * Checks if a new set of points is too similar to any existing saved gesture.
     */
    fun findSimilarGesture(
        normalizedPoints: List<GesturePoint>,
        excludeId: String? = null
    ): Pair<GestureAppBinding, Float>? {
        for (g in _gestures.value) {
            if (g.id == excludeId) continue
            val score = GestureRecognizer.computeSimilarity(normalizedPoints, g.normalizedPoints)
            if (score >= GestureRecognizer.SIMILARITY_WARNING_THRESHOLD) {
                return Pair(g, score)
            }
        }
        return null
    }

    companion object {
        private const val PREFS_NAME = "floating_eye_gestures_prefs"
        private const val KEY_GESTURES = "saved_gesture_bindings_json"
    }
}
