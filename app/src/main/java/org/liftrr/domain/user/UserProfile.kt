package org.liftrr.domain.user

import org.liftrr.domain.workout.ExerciseType


data class UserProfile(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val gender: Gender,
    val height: Float,
    val fitnessLevel: FitnessLevel,
    val dateOfBirth: Long,
    val weight: Float? = null,
    val goalsJson: String? = null,
    val preferredExercises: ExerciseType? = null,
    val preferredUnits: UnitSystem = UnitSystem.METRIC,
)
