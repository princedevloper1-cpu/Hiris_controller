package com.example.floatingeye.model

import android.content.Context
import org.json.JSONObject

data class FloatingEyeConfig(
    val modelName: String = "FloatingEye",
    val assetPath: String = "FloatingEye.glb",
    val trackingParts: List<String> = listOf(
        "05_Eye_Lens",
        "06_Iris",
        "07_Iris_Cyan_Ring",
        "08_Iris_Dark_Ring",
        "09_Pupil",
        "10_Highlight_Big",
        "11_Highlight_Small"
    ),
    val emissiveMaterial: String = "FE_NeonCyan",
    val defaultScale: Float = 1.2f,
    val maxDeflectionAngleXDeg: Float = 28f,
    val maxDeflectionAngleYDeg: Float = 34f,
    val trackingSpeed: Float = 14f,
    val bloomEnabled: Boolean = true,
    val bloomStrength: Float = 0.35f,
    val dragThresholdPx: Float = 12f,
    val tapMaxDurationMs: Long = 220L
) {
    companion object {
        fun loadFromAssets(context: Context, fileName: String = "floating_eye.json"): FloatingEyeConfig {
            return try {
                val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                
                val modelObj = root.optJSONObject("model")
                val partsArray = root.optJSONArray("trackingParts")
                val partsList = mutableListOf<String>()
                if (partsArray != null) {
                    for (i in 0 until partsArray.length()) {
                        partsList.add(partsArray.getString(i))
                    }
                }
                
                val materialsObj = root.optJSONObject("materials")
                val transformsObj = root.optJSONObject("transforms")
                val bloomObj = root.optJSONObject("bloom")
                val interactionObj = root.optJSONObject("interaction")
                
                FloatingEyeConfig(
                    modelName = modelObj?.optString("name", "FloatingEye") ?: "FloatingEye",
                    assetPath = modelObj?.optString("assetPath", "FloatingEye.glb") ?: "FloatingEye.glb",
                    trackingParts = if (partsList.isNotEmpty()) partsList else listOf(
                        "05_Eye_Lens", "06_Iris", "07_Iris_Cyan_Ring", "08_Iris_Dark_Ring",
                        "09_Pupil", "10_Highlight_Big", "11_Highlight_Small"
                    ),
                    emissiveMaterial = materialsObj?.optString("emissiveAccent", "FE_NeonCyan") ?: "FE_NeonCyan",
                    defaultScale = transformsObj?.optDouble("defaultScale", 1.2)?.toFloat() ?: 1.2f,
                    maxDeflectionAngleXDeg = transformsObj?.optDouble("maxDeflectionAngleXDeg", 28.0)?.toFloat() ?: 28f,
                    maxDeflectionAngleYDeg = transformsObj?.optDouble("maxDeflectionAngleYDeg", 34.0)?.toFloat() ?: 34f,
                    trackingSpeed = transformsObj?.optDouble("trackingInterpolationSpeed", 14.0)?.toFloat() ?: 14f,
                    bloomEnabled = bloomObj?.optBoolean("enabled", true) ?: true,
                    bloomStrength = bloomObj?.optDouble("strength", 0.35)?.toFloat() ?: 0.35f,
                    dragThresholdPx = interactionObj?.optDouble("dragThresholdPx", 12.0)?.toFloat() ?: 12f,
                    tapMaxDurationMs = interactionObj?.optLong("tapMaxDurationMs", 220L) ?: 220L
                )
            } catch (e: Exception) {
                // Fallback to defaults
                FloatingEyeConfig()
            }
        }
    }
}
