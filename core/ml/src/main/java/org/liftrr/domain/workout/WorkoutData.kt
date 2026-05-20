package org.liftrr.domain.workout

import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseQuality

/**
 * Data class representing the current workout state
 */
data class WorkoutState(
    val formFeedback: String,
    val poseQualityScore: Float,
    val repCount: Int,
    val reps: List<RepData>,
    val currentPose: PoseDetectionResult,
    val poseQuality: PoseQuality? = null
)

/**
 * Data class for rep statistics
 */
data class RepStats(
    val total: Int,
    val good: Int,
    val bad: Int
)

/**
 * Data class for workout summary
 */

data class WorkoutSummary(
    val elapsedTime: Long,
    val repStats: RepStats,
    val reps: List<RepData>,
    val averageFormScore: Float
)
/**
 * Data class for form feedback
 */
data class FormFeedback(
    val message: String,
    val errorType: String,
    val score: Int
)
