package org.liftrr.ml

enum class ExerciseType {
    SQUAT,
    DEADLIFT,
    BENCH_PRESS;

    fun displayName(): String = when (this) {
        SQUAT -> "Squat"
        DEADLIFT -> "Deadlift"
        BENCH_PRESS -> "Bench Press"
    }

    companion object {
        fun fromDisplayName(name: String): ExerciseType? = when (name.lowercase()) {
            "squat" -> SQUAT
            "deadlift" -> DEADLIFT
            "bench press" -> BENCH_PRESS
            else -> null
        }
    }
}
