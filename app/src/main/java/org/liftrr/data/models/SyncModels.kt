package org.liftrr.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sync status for entities that need to be synchronized with backend
 */
enum class SyncStatus {
    PENDING,    // Not yet synced to server
    SYNCING,    // Currently being synced
    SYNCED,     // Successfully synced
    FAILED      // Sync failed (will retry)
}

/**
 * Type of sync operation to perform
 */
enum class SyncOperation {
    INSERT,     // Create new entity on server
    UPDATE,     // Update existing entity on server
    DELETE      // Delete entity from server
}

/**
 * Queue of pending sync operations to be performed when online
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["entityType"]),
        Index(value = ["syncStatus"]),
        Index(value = ["createdAt"]),
        Index(value = ["entityType", "syncStatus"])
    ]
)
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val entityType: String,              // "workout", "user_profile", etc.
    val entityId: String,                // ID of the entity to sync
    val operation: SyncOperation,        // INSERT, UPDATE, DELETE
    val data: String,                    // JSON payload of the entity

    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,
    val retryCount: Int = 0,
    val maxRetries: Int = 5,

    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastError: String? = null,
    val errorDetails: String? = null
)
