package org.liftrr.data.repository

import org.liftrr.data.local.workout.WeightsDao
import org.liftrr.data.repository.mappers.toDomain
import org.liftrr.data.repository.mappers.toDto
import org.liftrr.domain.weight.UserWeight
import org.liftrr.domain.weight.UserWeightRepository
import org.liftrr.domain.workout.ExerciseType
import javax.inject.Inject

class UserWeightRepositoryImpl @Inject constructor(private val userWeightsDao: WeightsDao): UserWeightRepository {
    override suspend fun getUserWeights(): List<UserWeight> {
       return userWeightsDao.getWeights().map { it.toDomain() }
    }

    override suspend fun getUserWeight(exerciseType: ExerciseType): UserWeight? {
       return userWeightsDao.getWeights().firstOrNull { it.type == exerciseType }?.toDomain()
    }

    override suspend fun saveUserWeight(userWeight: UserWeight) {
        userWeightsDao.insertWeight(userWeight.toDto())
    }

    override suspend fun updateUserWeight(userWeight: UserWeight) {
        userWeightsDao.updateWeight(userWeight.toDto())
    }

    override suspend fun deleteUserWeight(exerciseType: ExerciseType) {
        getUserWeight(exerciseType)?.toDto()?.let {
            userWeightsDao.deleteWeight(it)
        }
    }
}