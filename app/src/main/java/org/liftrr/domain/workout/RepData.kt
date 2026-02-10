package org.liftrr.domain.workout

/**
 * Data class representing a single rep performed during a workout
 */
data class RepData(
    val repNumber: Int,
    val timestamp: Long,
    val poseQuality: Float,
    val isGoodForm: Boolean
)
