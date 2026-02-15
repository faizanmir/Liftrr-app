package org.liftrr.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.liftrr.data.models.SyncOperation
import org.liftrr.data.models.SyncQueueItem
import org.liftrr.data.models.SyncStatus

@Dao
interface SyncQueueDao {

    /**
     * Insert a new item into the sync queue
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueItem): Long

    /**
     * Insert multiple items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SyncQueueItem>)

    /**
     * Get all pending items ordered by creation time (FIFO)
     */
    @Query("SELECT * FROM sync_queue WHERE syncStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingItems(): List<SyncQueueItem>

    /**
     * Get pending items for a specific entity type
     */
    @Query("SELECT * FROM sync_queue WHERE syncStatus = 'PENDING' AND entityType = :entityType ORDER BY createdAt ASC")
    suspend fun getPendingItemsByType(entityType: String): List<SyncQueueItem>

    /**
     * Get failed items that haven't exceeded max retries
     */
    @Query("SELECT * FROM sync_queue WHERE syncStatus = 'FAILED' AND retryCount < maxRetries ORDER BY createdAt ASC")
    suspend fun getRetryableItems(): List<SyncQueueItem>

    /**
     * Get item by ID
     */
    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getItemById(id: Long): SyncQueueItem?

    /**
     * Get items for a specific entity
     */
    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId ORDER BY createdAt DESC")
    suspend fun getItemsForEntity(entityType: String, entityId: String): List<SyncQueueItem>

    /**
     * Observe sync queue size
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE syncStatus IN ('PENDING', 'SYNCING')")
    fun observePendingCount(): Flow<Int>

    /**
     * Update sync status
     */
    @Query("UPDATE sync_queue SET syncStatus = :status, lastAttemptAt = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark item as synced and remove from queue
     */
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    /**
     * Record sync failure
     */
    @Query("""
        UPDATE sync_queue
        SET syncStatus = 'FAILED',
            retryCount = retryCount + 1,
            lastAttemptAt = :timestamp,
            lastError = :error,
            errorDetails = :details
        WHERE id = :id
    """)
    suspend fun recordFailure(
        id: Long,
        error: String,
        details: String? = null,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Delete items that have exceeded max retries
     */
    @Query("DELETE FROM sync_queue WHERE syncStatus = 'FAILED' AND retryCount >= maxRetries")
    suspend fun deleteFailedItems()

    /**
     * Delete all synced items older than specified time
     */
    @Query("DELETE FROM sync_queue WHERE createdAt < :before")
    suspend fun deleteOldItems(before: Long)

    /**
     * Clear entire sync queue (use with caution)
     */
    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()

    /**
     * Get count of items by status
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE syncStatus = :status")
    suspend fun getCountByStatus(status: SyncStatus): Int

    /**
     * Check if there are pending operations for an entity
     */
    @Query("SELECT COUNT(*) > 0 FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId AND syncStatus IN ('PENDING', 'SYNCING')")
    suspend fun hasPendingOperations(entityType: String, entityId: String): Boolean

    /**
     * Delete specific sync queue item
     */
    @Delete
    suspend fun delete(item: SyncQueueItem)

    /**
     * Update retry count
     */
    @Query("UPDATE sync_queue SET retryCount = :count WHERE id = :id")
    suspend fun updateRetryCount(id: Long, count: Int)
}
