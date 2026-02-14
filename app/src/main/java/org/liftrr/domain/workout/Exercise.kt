package org.liftrr.domain.workout

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.liftrr.ml.PoseAnalyzer
import org.liftrr.ml.PoseDetectionResult

interface Exercise {
    fun analyzeFeedback(pose: PoseDetectionResult.Success): String
    fun updateRepCount(pose: PoseDetectionResult.Success): Boolean
    fun hadGoodForm(): Boolean
    fun formScore(): Float
    fun reset()
}

/**
 * Helper object for calculating angles using both sides of the body when available.
 * Falls back to the more visible side if one side has poor visibility.
 */
object BilateralAngleCalculator {
    private const val MIN_VISIBILITY_THRESHOLD = 0.5f

    /**
     * Calculate angle using both sides and average them, or use the more visible side.
     */
    fun calculateBilateralAngle(
        leftPoint1: NormalizedLandmark?,
        leftPoint2: NormalizedLandmark?,
        leftPoint3: NormalizedLandmark?,
        rightPoint1: NormalizedLandmark?,
        rightPoint2: NormalizedLandmark?,
        rightPoint3: NormalizedLandmark?
    ): Float? {
        val leftVisibility = getAverageVisibility(leftPoint1, leftPoint2, leftPoint3)
        val rightVisibility = getAverageVisibility(rightPoint1, rightPoint2, rightPoint3)

        val leftAngle = if (leftPoint1 != null && leftPoint2 != null && leftPoint3 != null) {
            PoseAnalyzer.calculateAngle(leftPoint1, leftPoint2, leftPoint3)
        } else null

        val rightAngle = if (rightPoint1 != null && rightPoint2 != null && rightPoint3 != null) {
            PoseAnalyzer.calculateAngle(rightPoint1, rightPoint2, rightPoint3)
        } else null

        return when {
            // Both sides visible and good quality - average them
            leftAngle != null && rightAngle != null &&
                leftVisibility >= MIN_VISIBILITY_THRESHOLD &&
                rightVisibility >= MIN_VISIBILITY_THRESHOLD -> {
                (leftAngle + rightAngle) / 2f
            }
            // Only left side is usable
            leftAngle != null && leftVisibility >= MIN_VISIBILITY_THRESHOLD -> leftAngle
            // Only right side is usable
            rightAngle != null && rightVisibility >= MIN_VISIBILITY_THRESHOLD -> rightAngle
            // Neither side meets threshold, use whichever is better
            leftVisibility > rightVisibility && leftAngle != null -> leftAngle
            rightAngle != null -> rightAngle
            leftAngle != null -> leftAngle
            else -> null
        }
    }

    /**
     * Get average visibility score for a set of landmarks.
     */
    private fun getAverageVisibility(vararg landmarks: NormalizedLandmark?): Float {
        val validLandmarks = landmarks.filterNotNull()
        if (validLandmarks.isEmpty()) return 0f

        return validLandmarks
            .mapNotNull { it.visibility()?.get() }
            .average()
            .toFloat()
    }
}

class AngleSmoother(private val windowSize: Int = 3) {
    private val buffer = FloatArray(windowSize)
    private var count = 0
    private var index = 0

    fun add(value: Float): Float {
        buffer[index] = value
        index = (index + 1) % windowSize
        if (count < windowSize) count++
        var sum = 0f
        for (i in 0 until count) sum += buffer[i]
        return sum / count
    }

    fun reset() {
        buffer.fill(0f)
        count = 0
        index = 0
    }
}
