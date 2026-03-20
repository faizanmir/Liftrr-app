package org.liftrr.domain.weight

import org.liftrr.domain.workout.ExerciseType

data class UserWeight(val exerciseType: ExerciseType, val weight: Float)