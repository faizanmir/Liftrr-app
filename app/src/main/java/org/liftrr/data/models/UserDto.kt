package org.liftrr.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AuthProvider {
    EMAIL_PASSWORD,
    GOOGLE,
    FACEBOOK,
    APPLE
}

enum class Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY
}

enum class FitnessLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    ELITE
}

enum class UnitSystem {
    METRIC,     // kg, cm
    IMPERIAL    // lbs, inches
}

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

    // Profile data
    val dateOfBirth: Long? = null,           // Timestamp
    val gender: Gender? = null,
    val height: Float? = null,               // In cm (always store in metric)
    val weight: Float? = null,               // In kg (always store in metric)
    val fitnessLevel: FitnessLevel? = null,

    // Goals and preferences
    val goalsJson: String? = null,           // JSON array of fitness goals
    val preferredExercises: String? = null,  // JSON array of exercise types
    val preferredUnits: UnitSystem = UnitSystem.METRIC,
    val notificationsEnabled: Boolean = true,
    val reminderTime: String? = null,        // e.g., "09:00" for 9 AM reminders

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