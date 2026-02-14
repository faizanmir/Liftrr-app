package org.liftrr.domain.workout

import org.liftrr.ml.ExerciseType

object ExerciseFactory {
    fun create(exerciseType: ExerciseType): Exercise {
        return when (exerciseType) {
            ExerciseType.SQUAT -> SquatExercise()
            ExerciseType.DEADLIFT -> DeadliftExercise()
            ExerciseType.BENCH_PRESS -> BenchPressExercise()
        }
    }
}
