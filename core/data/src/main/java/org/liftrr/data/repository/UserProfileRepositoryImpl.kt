package org.liftrr.data.repository

import org.liftrr.data.remote.UserProfileApiService
import org.liftrr.data.remote.dto.profile.UserProfileRequest
import org.liftrr.data.remote.dto.profile.UserProfileResponse
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
    private val userProfileApiService: UserProfileApiService
) : UserProfileRepository {

    override suspend fun getUserProfile(): UserProfile? {
        return try {
            userProfileApiService.getProfile().toDomain()
        } catch (e: HttpException) {
            if (e.code() == HTTP_NOT_FOUND) null else throw e
        }
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
        return true
    }

    override suspend fun deleteUserProfile(userProfile: UserProfile): Boolean {
        userProfileApiService.deleteProfile()
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
        return true
    }

    private fun UserProfileResponse.toDomain(): UserProfile = UserProfile(
        userId = userId,
        firstName = firstName ?: "",
        lastName = lastName ?: "",
        gender = gender.toEnumOrDefault(Gender.PREFER_NOT_TO_SAY),
        height = height ?: DEFAULT_HEIGHT_CM,
        fitnessLevel = fitnessLevel.toEnumOrDefault(FitnessLevel.BEGINNER),
        dateOfBirth = dateOfBirth ?: System.currentTimeMillis(),
        weight = weight,
        goalsJson = goalsJson,
        preferredExercises = preferredExercises.toEnumOrDefault(null),
        preferredUnits = preferredUnits.toEnumOrDefault(UnitSystem.METRIC)
    )

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
