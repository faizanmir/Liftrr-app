package org.liftrr.data.remote.dto.profile

data class UserProfileRequest(
    val firstName: String?,
    val lastName: String?,
    val photoUrl: String? = null,
    val gender: String? = null,
    val height: Float? = null,
    val fitnessLevel: String? = null,
    val dateOfBirth: Long? = null,
    val weight: Float? = null,
    val goalsJson: String? = null,
    val preferredExercises: String? = null,
    val preferredUnits: String? = null,
    val notificationsEnabled: Boolean? = null,
    val reminderTime: String? = null
)

data class UserProfileResponse(
    val userId: String,
    val profileId: String? = null,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val photoUrl: String? = null,
    val gender: String? = null,
    val height: Float? = null,
    val fitnessLevel: String? = null,
    val dateOfBirth: Long? = null,
    val weight: Float? = null,
    val goalsJson: String? = null,
    val preferredExercises: String? = null,
    val preferredUnits: String? = null,
    val notificationsEnabled: Boolean = true,
    val reminderTime: String? = null,
    val createdAt: String? = null,
    val modifiedAt: String? = null,
    val lastSyncedAt: String? = null,
    val version: Int = 0,
    val photoUploadUrl: String? = null
)
