package org.liftrr.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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

    // Core workout data
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

    // Local media
    val videoUri: String?,                  // Local file path
    val repDataJson: String? = null,
    val keyFramesJson: String? = null,       // Serialized list of KeyFrame objects

    // Cloud storage URLs
    val videoCloudUrl: String? = null,       // S3/Cloud Storage URL
    val keyFramesCloudUrls: String? = null,  // JSON array of cloud URLs

    // User association
    val userId: String,                       // Owner of this workout

    // Sync tracking
    val serverId: String? = null,            // Backend-assigned ID (null for local-only)
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncedAt: Long? = null,          // Timestamp of last successful sync
    val version: Int = 1,                    // For optimistic locking/conflict resolution

    // Soft delete
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,

    // Legacy support (deprecated, use syncStatus instead)
    @Deprecated("Use syncStatus instead")
    val isUploaded: Boolean = false
)
