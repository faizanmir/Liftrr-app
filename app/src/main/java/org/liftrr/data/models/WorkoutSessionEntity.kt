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
        Index(value = ["isDeleted"])
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
    val videoUri: String?,
    val weight: Float?,
    val timestamp: Long,
    val isUploaded: Boolean = false,
    val repDataJson: String? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
