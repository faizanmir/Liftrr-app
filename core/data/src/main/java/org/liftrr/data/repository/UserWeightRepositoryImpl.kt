package org.liftrr.data.repository

import org.liftrr.data.local.workout.UserWeightDto
import org.liftrr.data.local.workout.WeightsDao
import org.liftrr.data.remote.UserWeightApiService
import org.liftrr.data.remote.dto.weight.BulkWeightUpsertRequest
import org.liftrr.data.remote.dto.weight.WeightEntryRequest
import org.liftrr.data.repository.mappers.toDomain
import org.liftrr.data.repository.mappers.toDto
import org.liftrr.domain.weight.UserWeight
import org.liftrr.domain.weight.UserWeightRepository
import org.liftrr.domain.workout.ExerciseType
import javax.inject.Inject

class UserWeightRepositoryImpl @Inject constructor(
    private val userWeightsDao: WeightsDao,
    private val userWeightApiService: UserWeightApiService
) : UserWeightRepository {

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

    override suspend fun syncFromRemote(): Result<Unit> = runCatching {
        val remoteWeights = userWeightApiService.listWeights()
        remoteWeights.forEach { entry ->
            val type = ExerciseType.entries.firstOrNull {
                it.name.equals(entry.exerciseType, ignoreCase = true)
            } ?: return@forEach
            val local = userWeightsDao.getWeightByType(type)
            if (local == null || local.timestamp < entry.timestamp) {
                userWeightsDao.insertWeight(
                    UserWeightDto(type = type, weight = entry.weight, timestamp = entry.timestamp)
                )
            }
        }
    }

    override suspend fun syncToRemote(): Result<Unit> = runCatching {
        val localWeights = userWeightsDao.getWeights()
        if (localWeights.isEmpty()) return@runCatching
        userWeightApiService.bulkUpsert(
            BulkWeightUpsertRequest(
                entries = localWeights.map { dto ->
                    WeightEntryRequest(
                        exerciseType = dto.type.name,
                        weight = dto.weight,
                        timestamp = dto.timestamp
                    )
                }
            )
        )
    }
}