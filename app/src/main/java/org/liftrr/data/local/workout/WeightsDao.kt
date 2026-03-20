package org.liftrr.data.local.workout

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import org.liftrr.data.models.dto.UserWeightDto
import org.liftrr.domain.workout.ExerciseType

@Dao
interface WeightsDao {

    @Query("SELECT * FROM user_weights")
    suspend fun getWeights(): List<UserWeightDto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(userWeight: UserWeightDto)

    @Delete
    suspend fun deleteWeight(weight: UserWeightDto)

    @Update
    suspend fun updateWeight(weight: UserWeightDto)

    @Query("SELECT * FROM user_weights WHERE type = :type")
    suspend fun getWeightByType(type: ExerciseType): UserWeightDto?


}