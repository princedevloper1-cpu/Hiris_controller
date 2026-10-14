package com.example.floatingeye.gestures

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

sealed class RecognitionResult {
    data class Success(val binding: GestureAppBinding, val confidence: Float) : RecognitionResult()
    data class LowConfidence(val closest: GestureAppBinding?, val confidence: Float) : RecognitionResult()
    object InsufficientPoints : RecognitionResult()
}

/**
 * Ordered unistroke recognizer using equidistant resampling and translation/scale normalization.
 * Stroke order and direction are retained so unrelated paths are less likely to match.
 */
object GestureRecognizer {
    const val RESAMPLE_COUNT = 32
    // Require a close ordered-path match before launching an application.
    const val MINIMUM_RECOGNITION_CONFIDENCE = 0.72f
    const val MINIMUM_RECOGNITION_MARGIN = 0.10f
    const val SIMILARITY_WARNING_THRESHOLD = 0.80f

    /**
     * Normalizes raw captured points into standard resampled, bounding-box scaled, centroid-centered points.
     */
    fun normalize(points: List<GesturePoint>): List<GesturePoint> {
        if (points.size < 2) return emptyList()

        // 1. Resample to standard point count
        val resampled = resample(points, RESAMPLE_COUNT)
        if (resampled.size < 2) return emptyList()

        // 2. Compute bounding box
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (p in resampled) {
            minX = min(minX, p.x)
            maxX = max(maxX, p.x)
            minY = min(minY, p.y)
            maxY = max(maxY, p.y)
        }

        val width = max(maxX - minX, 1f)
        val height = max(maxY - minY, 1f)
        val maxDim = max(width, height)

        // 3. Scale and center on normalized [0, 1] x [0, 1] square
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f

        return resampled.map { p ->
            val normX = ((p.x - centerX) / maxDim) + 0.5f
            val normY = ((p.y - centerY) / maxDim) + 0.5f
            GesturePoint(normX.coerceIn(0f, 1f), normY.coerceIn(0f, 1f))
        }
    }

    /**
     * Resamples path to N equidistant points along the trajectory.
     * Guaranteed never to produce NaN, zero division, or infinite loops.
     */
    private fun resample(points: List<GesturePoint>, count: Int): List<GesturePoint> {
        if (points.size < 2) return points
        val totalLength = computePathLength(points)
        if (totalLength <= 0.001f) {
            return List(count) { points.first() }
        }

        val step = totalLength / (count - 1)
        val result = ArrayList<GesturePoint>(count)
        result.add(points.first())

        var currentDist = 0f
        var targetDist = step
        var pPrev = points[0]

        for (i in 1 until points.size) {
            val pCurr = points[i]
            val segDist = hypot(pCurr.x - pPrev.x, pCurr.y - pPrev.y)
            if (segDist <= 0.0001f) continue

            while (currentDist + segDist >= targetDist && result.size < count) {
                val remain = targetDist - currentDist
                val t = (remain / segDist).coerceIn(0f, 1f)
                val nx = pPrev.x + t * (pCurr.x - pPrev.x)
                val ny = pPrev.y + t * (pCurr.y - pPrev.y)
                result.add(GesturePoint(nx, ny))
                targetDist += step
            }
            currentDist += segDist
            pPrev = pCurr
        }

        while (result.size < count) {
            result.add(points.last())
        }

        return result
    }

    private fun computePathLength(points: List<GesturePoint>): Float {
        var length = 0f
        for (i in 1 until points.size) {
            length += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
        }
        return length
    }

    /**
     * Computes ordered-path similarity between two normalized paths.
     * Resampling already handles differences in drawing speed. Direction and point order
     * stay significant so a reversed or merely nearby path cannot win through path overlap.
     */
    fun computeSimilarity(pointsA: List<GesturePoint>, pointsB: List<GesturePoint>): Float {
        if (pointsA.size != pointsB.size || pointsA.isEmpty()) return 0f

        return computeDirectScore(pointsA, pointsB).coerceIn(0f, 1f)
    }

    private fun computeDirectScore(pointsA: List<GesturePoint>, pointsB: List<GesturePoint>): Float {
        var totalDist = 0f
        for (i in pointsA.indices) {
            val pa = pointsA[i]
            val pb = pointsB[i]
            totalDist += hypot(pa.x - pb.x, pa.y - pb.y)
        }

        val avgDist = totalDist / pointsA.size
        // Standard Dollar normalizer for unit square: 0.5 * sqrt(1^2 + 1^2) = 0.707f
        return (1f - (avgDist / 0.70f)).coerceIn(0f, 1f)
    }

    /**
     * Compares raw input points with registered gesture templates.
     */
    fun recognize(
        rawPoints: List<GesturePoint>,
        templates: List<GestureAppBinding>
    ): RecognitionResult {
        if (rawPoints.size < 4) {
            return RecognitionResult.InsufficientPoints
        }

        val normalizedInput = normalize(rawPoints)
        if (normalizedInput.isEmpty()) {
            return RecognitionResult.InsufficientPoints
        }

        val activeTemplates = templates.filter { it.isEnabled }
        if (activeTemplates.isEmpty()) {
            return RecognitionResult.LowConfidence(null, 0f)
        }

        val rankedMatches = activeTemplates
            .map { template ->
                template to computeSimilarity(normalizedInput, template.normalizedPoints)
            }
            .sortedByDescending { it.second }

        val (bestMatch, highestScore) = rankedMatches.first()
        val closestDifferentAppScore = rankedMatches
            .firstOrNull { it.first.packageName != bestMatch.packageName }
            ?.second ?: 0f
        val confidenceMargin = highestScore - closestDifferentAppScore

        return if (
            highestScore >= MINIMUM_RECOGNITION_CONFIDENCE &&
            confidenceMargin >= MINIMUM_RECOGNITION_MARGIN
        ) {
            RecognitionResult.Success(bestMatch, highestScore)
        } else {
            RecognitionResult.LowConfidence(bestMatch, highestScore.coerceAtLeast(0f))
        }
    }
}
