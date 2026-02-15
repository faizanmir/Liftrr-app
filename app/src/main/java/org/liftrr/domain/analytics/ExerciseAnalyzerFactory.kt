package org.liftrr.domain.analytics

import org.liftrr.ml.ExerciseType

object ExerciseAnalyzerFactory {
    fun create(exerciseType: ExerciseType): ExerciseSpecificAnalyzer {
        return when (exerciseType) {
            ExerciseType.SQUAT -> SquatAnalyzer()
            ExerciseType.DEADLIFT -> DeadliftAnalyzer()
            ExerciseType.BENCH_PRESS -> BenchPressAnalyzer()
        }
    }
}
