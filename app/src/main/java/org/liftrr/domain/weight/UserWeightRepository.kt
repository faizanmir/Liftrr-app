package org.liftrr.domain.weight

import org.liftrr.domain.workout.ExerciseType

interface UserWeightRepository {
    suspend fun getUserWeights(): List<UserWeight>
    suspend fun getUserWeight(exerciseType: ExerciseType): UserWeight?
    suspend fun saveUserWeight(userWeight: UserWeight)
    suspend fun updateUserWeight(userWeight: UserWeight)
    suspend fun deleteUserWeight(exerciseType: ExerciseType)
}


