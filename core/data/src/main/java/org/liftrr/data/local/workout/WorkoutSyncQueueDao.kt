package org.liftrr.data.local.workout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.liftrr.data.models.dto.WorkoutSyncOperation
import org.liftrr.data.models.dto.WorkoutSyncQueueEntity
import org.liftrr.data.models.dto.WorkoutSyncState

@Dao
interface WorkoutSyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(queueItem: WorkoutSyncQueueEntity)

    @Query(
        """
        SELECT * FROM workout_sync_queue
        WHERE state IN (:eligibleStates)
            AND nextEligibleAt <= :now
        ORDER BY createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun getDueQueueItems(
        now: Long,
        eligibleStates: List<WorkoutSyncState> = listOf(
            WorkoutSyncState.PENDING,
            WorkoutSyncState.FAILED
        ),
        limit: Int = 25
    ): List<WorkoutSyncQueueEntity>

    @Query(
        """
        UPDATE workout_sync_queue
        SET state = :state,
            updatedAt = :updatedAt,
            lastError = NULL
        WHERE queueId = :queueId
        """
    )
    suspend fun updateState(
        queueId: String,
        state: WorkoutSyncState,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE workout_sync_queue
        SET state = :state,
            attemptCount = :attemptCount,
            lastError = :lastError,
            updatedAt = :updatedAt,
            nextEligibleAt = :nextEligibleAt
        WHERE queueId = :queueId
        """
    )
    suspend fun markFailed(
        queueId: String,
        state: WorkoutSyncState,
        attemptCount: Int,
        lastError: String?,
        updatedAt: Long,
        nextEligibleAt: Long
    )

    @Query(
        """
        UPDATE workout_sync_queue
        SET state = :state,
            updatedAt = :updatedAt,
            lastError = NULL
        WHERE sessionId = :sessionId
            AND operation = :operation
            AND state != :state
        """
    )
    suspend fun markOperationSucceeded(
        sessionId: String,
        operation: WorkoutSyncOperation,
        state: WorkoutSyncState = WorkoutSyncState.SUCCEEDED,
        updatedAt: Long
    )

    @Query(
        """
        SELECT COUNT(*) FROM workout_sync_queue
        WHERE sessionId = :sessionId
            AND operation = :operation
            AND state != :succeeded
        """
    )
    suspend fun countIncompleteOperation(
        sessionId: String,
        operation: WorkoutSyncOperation,
        succeeded: WorkoutSyncState = WorkoutSyncState.SUCCEEDED
    ): Int
}
