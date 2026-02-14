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

    /**
     * Detect the current phase of the movement based on joint angles
     * Used for capturing key frames at different phases of the lift
     */
    fun detectMovementPhase(pose: PoseDetectionResult.Success): MovementPhase

    /**
     * Get detailed form diagnostics with specific angle measurements and issues
     * Used for providing actionable feedback in reports
     */
    fun getFormDiagnostics(pose: PoseDetectionResult.Success): List<FormDiagnostic>
}

/**
 * Helper object for calculating angles using both sides of the body when available.
 * Falls back to the more visible side if one side has poor visibility.
 *
 * This is especially useful when:
 * - Camera is positioned to one side (one leg may be occluded)
 * - User is partially out of frame
 * - Lighting conditions affect landmark detection on one side
 */
object BilateralAngleCalculator {
    // Balanced threshold (40%) - strict enough to reject bad occlusion data
    // but lenient enough to handle partial visibility
    private const val MIN_VISIBILITY_THRESHOLD = 0.4f

    // Minimum landmarks needed - if we have at least this many, attempt tracking
    private const val MIN_LANDMARK_CONFIDENCE = 0.25f

    // Angle validation - reject clearly impossible angles
    private const val MIN_VALID_ANGLE = 0f
    private const val MAX_VALID_ANGLE = 200f

    /**
     * Calculate angle using both sides and average them, or use the more visible side.
     *
     * Strategy:
     * 1. If both sides are clearly visible (>30% confidence): average them
     * 2. If only one side is clearly visible: use that side
     * 3. If both have poor visibility: use whichever is better (even if low confidence)
     * 4. Returns null only if no landmarks are detected at all
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

        // Calculate angles and validate they're within possible range
        val leftAngle = if (leftPoint1 != null && leftPoint2 != null && leftPoint3 != null) {
            val angle = PoseAnalyzer.calculateAngle(leftPoint1, leftPoint2, leftPoint3)
            // Reject impossible angles (likely from occlusion errors)
            if (angle in MIN_VALID_ANGLE..MAX_VALID_ANGLE) angle else null
        } else null

        val rightAngle = if (rightPoint1 != null && rightPoint2 != null && rightPoint3 != null) {
            val angle = PoseAnalyzer.calculateAngle(rightPoint1, rightPoint2, rightPoint3)
            // Reject impossible angles (likely from occlusion errors)
            if (angle in MIN_VALID_ANGLE..MAX_VALID_ANGLE) angle else null
        } else null

        return when {
            // Both sides visible and good quality - average them for best accuracy
            leftAngle != null && rightAngle != null &&
                leftVisibility >= MIN_VISIBILITY_THRESHOLD &&
                rightVisibility >= MIN_VISIBILITY_THRESHOLD -> {
                (leftAngle + rightAngle) / 2f
            }
            // Only left side meets quality threshold
            leftAngle != null && leftVisibility >= MIN_VISIBILITY_THRESHOLD -> leftAngle
            // Only right side meets quality threshold
            rightAngle != null && rightVisibility >= MIN_VISIBILITY_THRESHOLD -> rightAngle
            // Both have low visibility - use whichever is better (handles occlusion cases)
            // This ensures we can still track even when one leg is hidden
            leftVisibility > rightVisibility && leftAngle != null &&
                leftVisibility >= MIN_LANDMARK_CONFIDENCE -> leftAngle
            rightAngle != null && rightVisibility >= MIN_LANDMARK_CONFIDENCE -> rightAngle
            // Last resort: use any available angle even with very low confidence
            leftAngle != null -> leftAngle
            rightAngle != null -> rightAngle
            // No landmarks detected at all
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

        if (count == 0) return value

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
