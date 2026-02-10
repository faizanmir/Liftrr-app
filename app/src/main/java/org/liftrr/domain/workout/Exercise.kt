package org.liftrr.domain.workout

import org.liftrr.ml.PoseDetectionResult

/**
 * Interface for exercise-specific logic
 * Each exercise type implements rep counting and form analysis
 */
interface Exercise {
    fun analyzeFeedback(pose: PoseDetectionResult.Success): String
    fun updateRepCount(pose: PoseDetectionResult.Success): Boolean
    fun hadGoodForm(): Boolean

    /**
     * Returns a 0-100 form score for the last completed rep.
     * 100 = perfect form, penalties are subtracted for deviations.
     */
    fun formScore(): Float

    fun reset()
}

/**
 * Moving-average angle smoother to reduce jitter from noisy pose estimation.
 * Maintains a circular buffer of the last [windowSize] values.
 */
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
