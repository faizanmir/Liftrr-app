package org.liftrr.data.repository.mappers

import org.liftrr.data.models.dto.SyncStatus
import org.liftrr.data.models.dto.WorkoutSessionEntity
import org.liftrr.domain.workout.WorkoutRecord

fun WorkoutSessionEntity.toDomain(): WorkoutRecord = WorkoutRecord(
    sessionId = sessionId,
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
    videoUri = videoUri,
    repDataJson = repDataJson,
    keyFramesJson = keyFramesJson,
    videoCloudUrl = videoCloudUrl,
    keyFramesCloudUrls = keyFramesCloudUrls,
    userId = userId,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun WorkoutRecord.toEntity(
    syncStatus: SyncStatus = SyncStatus.PENDING
): WorkoutSessionEntity = WorkoutSessionEntity(
    sessionId = sessionId,
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
    videoUri = videoUri,
    repDataJson = repDataJson,
    keyFramesJson = keyFramesJson,
    videoCloudUrl = videoCloudUrl,
    keyFramesCloudUrls = keyFramesCloudUrls,
    userId = userId,
    syncStatus = syncStatus,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)
