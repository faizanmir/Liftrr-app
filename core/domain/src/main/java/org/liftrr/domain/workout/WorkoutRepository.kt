package org.liftrr.domain.workout

import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getAllWorkouts(): Flow<List<WorkoutRecord>>
    fun getRecentWorkouts(limit: Int = 10): Flow<List<WorkoutRecord>>
    fun getTodayWorkouts(): Flow<List<WorkoutRecord>>
    fun getWorkoutsSince(startTime: Long): Flow<List<WorkoutRecord>>
    fun getWeekWorkouts(): Flow<List<WorkoutRecord>>
    fun getMonthWorkouts(): Flow<List<WorkoutRecord>>
    suspend fun getWorkoutById(sessionId: String): WorkoutRecord?
    suspend fun saveWorkout(workout: WorkoutRecord)
    suspend fun markWorkoutAsDeleted(sessionId: String, deletedAt: Long = System.currentTimeMillis())
    suspend fun restoreWorkout(sessionId: String)
    suspend fun getDeletedWorkouts(): List<WorkoutRecord>
    suspend fun permanentlyDeleteWorkout(sessionId: String)
    suspend fun permanentlyDeleteAllMarkedWorkouts()
    suspend fun deleteWorkout(workout: WorkoutRecord)
    suspend fun deleteAllWorkouts()
    fun getTodayTotalReps(): Flow<Int>
    fun getTodayAverageQuality(): Flow<Float>
    fun getTodayTotalDuration(): Flow<Long>
    fun getTodayWorkoutCountByType(): Flow<Map<String, Int>>
    fun getWeeklyStats(): Flow<WorkoutStats>
}

data class WorkoutStats(
    val totalWorkouts: Int = 0,
    val totalReps: Int = 0,
    val totalDuration: Long = 0,
    val averageQuality: Float = 0f,
    val workoutsByType: Map<String, Int> = emptyMap(),
    val bestGrade: String? = null,
    val averageScore: Float = 0f
)
