package org.liftrr.data.repository

import org.liftrr.data.local.user.UserProfileDao
import org.liftrr.data.repository.mappers.toDomain
import org.liftrr.data.repository.mappers.toDto
import org.liftrr.domain.auth.AuthRepository
import org.liftrr.domain.user.FitnessLevel
import org.liftrr.domain.user.Gender
import org.liftrr.domain.user.UserProfile
import org.liftrr.domain.user.UserProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val userRepository: AuthRepository
) : UserProfileRepository {

    override suspend fun getUserProfile(): UserProfile? {
        userRepository.isUserSignedIn()
        val userId =
            userRepository.getCurrentUserOnce()?.userId ?: throw Exception("User not signed in")
        return userProfileDao.getProfileByUserId(userId)?.toDomain()
    }

    override suspend fun insertUserProfile(userProfile: UserProfile): Boolean {
       userProfileDao.insert(   userProfile.toDto())
        return true
    }

    override suspend fun deleteUserProfile(userProfile: UserProfile): Boolean {
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
        userProfileDao.updateFields(
            userId = userRepository.getCurrentUserOnce()?.userId
                ?: throw Exception("User not signed in"),
            firstName = firstName,
            lastName = lastName,
            gender = gender,
            height = height,
            fitnessLevel = fitnessLevel,
            goalsJson = goalsJson
        )
        return true
    }
}