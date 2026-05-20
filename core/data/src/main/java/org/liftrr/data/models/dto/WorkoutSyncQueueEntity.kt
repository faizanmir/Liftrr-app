package org.liftrr.data.models.dto

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class WorkoutSyncOperation {
    UPSERT_WORKOUT,
    DELETE_WORKOUT
}

enum class WorkoutSyncState {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED
}

@Entity(
    tableName = "workout_sync_queue",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["state", "nextEligibleAt"]),
        Index(value = ["operation"])
    ]
)
data class WorkoutSyncQueueEntity(
    @PrimaryKey
    val queueId: String,
    val sessionId: String,
    val operation: WorkoutSyncOperation,
    val state: WorkoutSyncState = WorkoutSyncState.PENDING,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val nextEligibleAt: Long = 0L
) {
    companion object {
        fun queueId(sessionId: String, operation: WorkoutSyncOperation): String =
            "$sessionId:${operation.name}"
    }
}
