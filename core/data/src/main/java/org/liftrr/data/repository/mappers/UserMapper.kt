package org.liftrr.data.repository.mappers

import org.liftrr.data.models.dto.SyncStatus
import org.liftrr.data.models.dto.UserDto
import org.liftrr.data.models.dto.UserProfileEntity
import org.liftrr.domain.user.User
import org.liftrr.domain.user.UserProfile



fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    firstName = firstName,
    lastName = lastName,
    gender = gender,
    height = height,
    fitnessLevel = fitnessLevel,
    dateOfBirth = dob,
    weight = weight,
    goalsJson = goalsJson,
    preferredExercises = preferredExercises,
    preferredUnits = preferredUnits,
    userId = userId,
)

fun UserProfile.toDto(): UserProfileEntity = UserProfileEntity(
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    gender = gender,
    height = height,
    fitnessLevel = fitnessLevel,
    dob = dateOfBirth,
    weight = weight,
    goalsJson = goalsJson,
    preferredExercises = preferredExercises,
    preferredUnits = preferredUnits,
    updatedAt = System.currentTimeMillis()
)


fun User.toDto(
    passwordHash: String? = null,
    syncStatus: SyncStatus = SyncStatus.SYNCED
): UserDto = UserDto(
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    email = email,
    passwordHash = passwordHash,
    authProvider = authProvider,
    photoUrl = photoUrl,
    photoCloudUrl = photoCloudUrl,
    createdAt = createdAt,
    lastLoginAt = lastLoginAt,
    syncStatus = syncStatus
)

    fun UserDto.toDomain(): User = User(
        userId = userId,
        firstName = firstName,
        lastName = lastName,
        email = email,
        authProvider = authProvider,
        photoUrl = photoUrl,
        photoCloudUrl = photoCloudUrl,
        createdAt = createdAt,
        lastLoginAt = lastLoginAt
    )
