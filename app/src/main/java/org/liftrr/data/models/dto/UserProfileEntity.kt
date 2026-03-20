package org.liftrr.data.models.dto

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.liftrr.domain.user.FitnessLevel
import org.liftrr.domain.user.Gender
import org.liftrr.domain.user.UnitSystem
import org.liftrr.domain.workout.ExerciseType

@Entity(
    tableName = "user_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserDto::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"], unique = true)]
)
data class UserProfileEntity(
    @PrimaryKey
    val userId: String,

    val firstName: String,
    val lastName: String,
    val gender: Gender,
    val height: Float,
    val fitnessLevel: FitnessLevel,
    val dob: Long,

    val dateOfBirth: Long? = null,
    val weight: Float? = null,

    val goalsJson: String? = null,
    val preferredExercises: ExerciseType? = null,
    val preferredUnits: UnitSystem = UnitSystem.METRIC,
    val notificationsEnabled: Boolean = true,
    val reminderTime: String? = null,

    val updatedAt: Long = System.currentTimeMillis()
)
