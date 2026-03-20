package org.liftrr.data.models.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.liftrr.domain.workout.ExerciseType

@Entity(tableName = "user_weights")
data class UserWeightDto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ExerciseType, val weight : Float)
