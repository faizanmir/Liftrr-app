package org.liftrr.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["timestamp"]),  // For ORDER BY timestamp queries
        Index(value = ["exerciseType"]),  // For filtering by exercise
        Index(value = ["timestamp", "exerciseType"])  // For combined queries
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val exerciseType: String,  // SQUAT, DEADLIFT, BENCH_PRESS
    val totalReps: Int,
    val goodReps: Int,
    val badReps: Int,
    val averageQuality: Float,
    val durationMs: Long,
    val overallScore: Float,
    val grade: String,
    val videoUri: String?,  // Local video file URI
    val weight: Float?,  // Weight used in kg (null if not specified)
    val timestamp: Long,  // When workout was completed
    val isUploaded: Boolean = false,  // For future cloud uploads
    val repDataJson: String? = null  // JSON array of rep data for detailed analysis
)
