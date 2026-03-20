package org.liftrr.domain.workout

object ExerciseFactory {
    fun create(exerciseType: ExerciseType): Exercise {
        return when (exerciseType) {
            ExerciseType.SQUAT -> SquatExercise()
            ExerciseType.DEADLIFT -> DeadliftExercise()
            ExerciseType.BENCH_PRESS -> BenchPressExercise()
        }
    }
}
