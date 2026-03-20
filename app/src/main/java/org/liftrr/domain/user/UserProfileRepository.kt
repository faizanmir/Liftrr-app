package org.liftrr.domain.user

interface UserProfileRepository {
    suspend fun getUserProfile(): UserProfile?

    suspend fun insertUserProfile(userProfile: UserProfile): Boolean

    suspend fun deleteUserProfile(userProfile: UserProfile): Boolean

    suspend fun updateUserProfile(
        firstName: String?,
        lastName: String?,
        gender: Gender?,
        height: Float?,
        fitnessLevel: FitnessLevel?,
        goalsJson: String?
    ): Boolean
}
