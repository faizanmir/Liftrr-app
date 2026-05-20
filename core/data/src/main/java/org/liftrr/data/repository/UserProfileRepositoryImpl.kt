package org.liftrr.data.repository

import org.liftrr.data.local.user.UserProfileDao
import org.liftrr.data.models.dto.UserProfileEntity
import org.liftrr.data.remote.UserProfileApiService
import org.liftrr.data.remote.dto.profile.UserProfileRequest
import org.liftrr.data.remote.dto.profile.UserProfileResponse
import org.liftrr.data.repository.mappers.toDomain
import org.liftrr.data.repository.mappers.toDto
import org.liftrr.domain.auth.AuthRepository
import org.liftrr.domain.user.FitnessLevel
import org.liftrr.domain.user.Gender
import org.liftrr.domain.user.UnitSystem
import org.liftrr.domain.user.UserProfile
import org.liftrr.domain.user.UserProfileRepository
import org.liftrr.domain.workout.ExerciseType
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val userRepository: AuthRepository,
    private val userProfileApiService: UserProfileApiService
) : UserProfileRepository {

    override suspend fun getUserProfile(): UserProfile? {
        val userId = currentUserId()
        val localProfile = userProfileDao.getProfileByUserId(userId)
        val remoteProfile = try {
            userProfileApiService.getProfile()
        } catch (e: HttpException) {
            if (e.code() == HTTP_NOT_FOUND) return localProfile?.toDomain()
            throw e
        }

        val mergedProfile = remoteProfile.toLocalEntity(
            userId = userId,
            fallback = localProfile
        )
        userProfileDao.insert(mergedProfile)
        return mergedProfile.toDomain()
    }

    override suspend fun insertUserProfile(userProfile: UserProfile): Boolean {
        val request = userProfile.toRemoteRequest()
        try {
            userProfileApiService.createProfile(request)
        } catch (e: HttpException) {
            if (e.code() == HTTP_CONFLICT) {
                userProfileApiService.updateProfile(request)
            } else {
                throw e
            }
        }
        userProfileDao.insert(userProfile.toDto())
        return true
    }

    override suspend fun deleteUserProfile(userProfile: UserProfile): Boolean {
        userProfileApiService.deleteProfile()
        userProfileDao.deleteByUserId(userProfile.userId)
        return true
    }

    override suspend fun updateUserProfile(
        firstName: String?,
        lastName: String?,
        gender: Gender?,
        height: Float?,
        fitnessLevel: FitnessLevel?,
        goalsJson: String?
    ): Boolean {
        userProfileApiService.updateProfile(
            UserProfileRequest(
                firstName = firstName,
                lastName = lastName,
                gender = gender?.name,
                height = height,
                fitnessLevel = fitnessLevel?.name,
                goalsJson = goalsJson
            )
        )
        userProfileDao.updateFields(
            userId = currentUserId(),
            firstName = firstName,
            lastName = lastName,
            gender = gender,
            height = height,
            fitnessLevel = fitnessLevel,
            goalsJson = goalsJson
        )
        return true
    }

    private suspend fun currentUserId(): String =
        userRepository.getCurrentUserOnce()?.userId ?: throw Exception("User not signed in")

    private fun UserProfile.toRemoteRequest(): UserProfileRequest =
        UserProfileRequest(
            firstName = firstName.ifBlank { null },
            lastName = lastName.ifBlank { null },
            gender = gender.name,
            height = height,
            fitnessLevel = fitnessLevel.name,
            dateOfBirth = dateOfBirth,
            weight = weight,
            goalsJson = goalsJson,
            preferredExercises = preferredExercises?.name,
            preferredUnits = preferredUnits.name
        )

    private fun UserProfileResponse.toLocalEntity(
        userId: String,
        fallback: UserProfileEntity?
    ): UserProfileEntity =
        UserProfileEntity(
            userId = userId,
            firstName = firstName ?: fallback?.firstName.orEmpty(),
            lastName = lastName ?: fallback?.lastName.orEmpty(),
            gender = gender.toEnumOrDefault(fallback?.gender ?: Gender.PREFER_NOT_TO_SAY),
            height = height ?: fallback?.height ?: DEFAULT_HEIGHT_CM,
            fitnessLevel = fitnessLevel.toEnumOrDefault(fallback?.fitnessLevel ?: FitnessLevel.BEGINNER),
            dob = dateOfBirth ?: fallback?.dob ?: System.currentTimeMillis(),
            weight = weight ?: fallback?.weight,
            goalsJson = goalsJson ?: fallback?.goalsJson,
            preferredExercises = preferredExercises.toEnumOrDefault(fallback?.preferredExercises),
            preferredUnits = preferredUnits.toEnumOrDefault(fallback?.preferredUnits ?: UnitSystem.METRIC),
            notificationsEnabled = notificationsEnabled,
            reminderTime = reminderTime ?: fallback?.reminderTime,
            updatedAt = System.currentTimeMillis()
        )

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default

    private fun String?.toEnumOrDefault(default: ExerciseType?): ExerciseType? =
        this?.let { value -> enumValues<ExerciseType>().firstOrNull { it.name == value } } ?: default

    private companion object {
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val DEFAULT_HEIGHT_CM = 170f
    }
}
