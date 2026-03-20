package org.liftrr.domain.workout

data class WorkoutRecord(
    val sessionId: String,
    val exerciseType: String,
    val totalReps: Int,
    val goodReps: Int,
    val badReps: Int,
    val averageQuality: Float,
    val durationMs: Long,
    val overallScore: Float,
    val grade: String,
    val weight: Float?,
    val timestamp: Long,
    val videoUri: String?,
    val repDataJson: String? = null,
    val keyFramesJson: String? = null,
    val videoCloudUrl: String? = null,
    val keyFramesCloudUrls: String? = null,
    val userId: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
