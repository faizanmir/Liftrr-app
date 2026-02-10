package org.liftrr.domain.workout

import org.liftrr.ml.ExerciseType

/**
 * Factory for creating Exercise instances based on ExerciseType
 * Follows Open/Closed Principle - can add new exercises without modifying existing code
 */
object ExerciseFactory {
    fun create(exerciseType: ExerciseType): Exercise {
        return when (exerciseType) {
            ExerciseType.SQUAT -> SquatExercise()
            ExerciseType.DEADLIFT -> DeadliftExercise()
            ExerciseType.BENCH_PRESS -> BenchPressExercise()
        }
    }
}
