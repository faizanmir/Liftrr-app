package org.liftrr.domain.workout

import org.liftrr.ml.PoseDetectionResult

/**
 * Interface for exercise-specific logic
 * Each exercise type implements rep counting and form analysis
 */
interface Exercise {
    /**
     * Analyze the current pose and return form feedback
     */
    fun analyzeFeedback(pose: PoseDetectionResult.Success): String

    /**
     * Update rep count based on current pose
     * Returns new count if a rep was completed, null otherwise
     */
    fun updateRepCount(pose: PoseDetectionResult.Success): Boolean

    /**
     * Check if the last rep had good form
     * Returns true if form was acceptable, false if form was poor
     */
    fun hadGoodForm(): Boolean

    /**
     * Reset exercise state (for new workout session)
     */
    fun reset()
}
