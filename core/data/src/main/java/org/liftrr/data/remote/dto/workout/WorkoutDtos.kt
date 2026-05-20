package org.liftrr.data.remote.dto.workout

import org.liftrr.data.models.dto.WorkoutSessionEntity

data class CreateWorkoutSessionRequest(
    val clientSessionId: String,
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
    val repDataJson: String?,
    val keyFramesJson: String?
)

data class WorkoutSessionResponse(
    val id: String? = null,
    val sessionId: String? = null,
    val clientSessionId: String,
    val exerciseType: String? = null,
    val totalReps: Int? = null,
    val goodReps: Int? = null,
    val badReps: Int? = null,
    val averageQuality: Float? = null,
    val durationMs: Long? = null,
    val overallScore: Float? = null,
    val grade: String? = null,
    val weight: Float? = null,
    val timestamp: Long? = null,
    val videoCloudUrl: String? = null,
    val keyFramesCloudUrls: String? = null,
    val lastSyncedAt: Long? = null,
    val version: Int? = null
) {
    val backendSessionId: String?
        get() = id ?: sessionId
}

data class WorkoutVideoUploadUrlRequest(
    val sessionId: String,
    val clientSessionId: String,
    val contentType: String = "video/mp4",
    val fileName: String? = null
)

data class WorkoutVideoUploadUrlResponse(
    val uploadUrl: String,
    val videoCloudUrl: String? = null,
    val contentType: String = "video/mp4"
)

data class ConfirmWorkoutVideoUploadRequest(
    val sessionId: String,
    val clientSessionId: String,
    val videoCloudUrl: String? = null
)

fun WorkoutSessionEntity.toCreateWorkoutSessionRequest(): CreateWorkoutSessionRequest =
    CreateWorkoutSessionRequest(
        clientSessionId = sessionId,
        exerciseType = exerciseType,
        totalReps = totalReps,
        goodReps = goodReps,
        badReps = badReps,
        averageQuality = averageQuality,
        durationMs = durationMs,
        overallScore = overallScore,
        grade = grade,
        weight = weight,
        timestamp = timestamp,
        repDataJson = repDataJson,
        keyFramesJson = keyFramesJson
    )
