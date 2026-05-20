package org.liftrr.data.local.workout

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.liftrr.data.local.SyncStatus

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["exerciseType"]),
        Index(value = ["timestamp", "exerciseType"]),
        Index(value = ["isDeleted"]),
        Index(value = ["userId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["userId", "timestamp"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey
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

    val serverId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncedAt: Long? = null,
    val version: Int = 1,

    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
)
