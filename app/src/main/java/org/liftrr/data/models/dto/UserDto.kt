package org.liftrr.data.models.dto

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.liftrr.domain.user.AuthProvider

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["syncStatus"])
    ]
)
data class UserDto(
    @PrimaryKey
    val userId: String,

    // Authentication
    val firstName: String?,
    val lastName: String?,
    val email: String,
    val passwordHash: String? = null,
    val authProvider: AuthProvider,
    val photoUrl: String? = null,
    val photoCloudUrl: String? = null,       // Cloud storage URL for profile photo

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Sync tracking
    val serverId: String? = null,            // Backend user ID (may differ from local)
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastSyncedAt: Long? = null,
    val version: Int = 1
)
