package org.liftrr.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.liftrr.data.local.WorkoutDao
import org.liftrr.data.models.WorkoutSessionEntity
import java.util.Calendar
import javax.inject.Inject

/**
 * Repository for managing workout sessions
 * Provides abstraction layer between data source and UI
 */
interface WorkoutRepository {
    /**
     * Get all workout sessions ordered by timestamp descending
     */
    fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>>

    /**
     * Get recent workout sessions with limit
     */
    fun getRecentWorkouts(limit: Int = 10): Flow<List<WorkoutSessionEntity>>

    /**
     * Get today's workout sessions
     */
    fun getTodayWorkouts(): Flow<List<WorkoutSessionEntity>>

    /**
     * Get workouts since a specific timestamp
     */
    fun getWorkoutsSince(startTime: Long): Flow<List<WorkoutSessionEntity>>

    /**
     * Get workouts from the last 7 days
     */
    fun getWeekWorkouts(): Flow<List<WorkoutSessionEntity>>

    /**
     * Get workouts from the current month
     */
    fun getMonthWorkouts(): Flow<List<WorkoutSessionEntity>>

    /**
     * Get a specific workout by ID
     */
    suspend fun getWorkoutById(sessionId: String): WorkoutSessionEntity?

    /**
     * Save a workout session
     */
    suspend fun saveWorkout(workout: WorkoutSessionEntity)

    /**
     * Delete a workout session
     */
    suspend fun deleteWorkout(workout: WorkoutSessionEntity)

    /**
     * Delete all workout sessions
     */
    suspend fun deleteAllWorkouts()

    /**
     * Get total reps completed today
     */
    fun getTodayTotalReps(): Flow<Int>

    /**
     * Get today's average workout quality
     */
    fun getTodayAverageQuality(): Flow<Float>

    /**
     * Get total workout duration today in milliseconds
     */
    fun getTodayTotalDuration(): Flow<Long>

    /**
     * Get workout count by exercise type for today
     */
    fun getTodayWorkoutCountByType(): Flow<Map<String, Int>>

    /**
     * Get weekly statistics
     */
    fun getWeeklyStats(): Flow<WorkoutStats>
}

/**
 * Statistics for workout analysis
 */
data class WorkoutStats(
    val totalWorkouts: Int = 0,
    val totalReps: Int = 0,
    val totalDuration: Long = 0,
    val averageQuality: Float = 0f,
    val workoutsByType: Map<String, Int> = emptyMap(),
    val bestGrade: String? = null,
    val averageScore: Float = 0f
)

/**
 * Implementation of WorkoutRepository
 */
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {

    override fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>> {
        return workoutDao.getAllWorkouts()
    }

    override fun getRecentWorkouts(limit: Int): Flow<List<WorkoutSessionEntity>> {
        return workoutDao.getRecentWorkouts(limit)
    }

    override fun getTodayWorkouts(): Flow<List<WorkoutSessionEntity>> {
        return workoutDao.getTodayWorkouts()
    }

    override fun getWorkoutsSince(startTime: Long): Flow<List<WorkoutSessionEntity>> {
        return workoutDao.getWorkoutsSince(startTime)
    }

    override fun getWeekWorkouts(): Flow<List<WorkoutSessionEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -7)

        // Use limited query to avoid loading excessive data
        return workoutDao.getWorkoutsSinceWithLimit(calendar.timeInMillis, limit = 100)
    }

    override fun getMonthWorkouts(): Flow<List<WorkoutSessionEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return workoutDao.getWorkoutsSince(calendar.timeInMillis)
    }

    override suspend fun getWorkoutById(sessionId: String): WorkoutSessionEntity? {
        return workoutDao.getWorkoutById(sessionId)
    }

    override suspend fun saveWorkout(workout: WorkoutSessionEntity) {
        workoutDao.insertWorkout(workout)
    }

    override suspend fun deleteWorkout(workout: WorkoutSessionEntity) {
        workoutDao.deleteWorkout(workout)
    }

    override suspend fun deleteAllWorkouts() {
        workoutDao.deleteAllWorkouts()
    }

    override fun getTodayTotalReps(): Flow<Int> {
        return getTodayWorkouts().map { workouts ->
            workouts.sumOf { it.totalReps }
        }
    }

    override fun getTodayAverageQuality(): Flow<Float> {
        return getTodayWorkouts().map { workouts ->
            if (workouts.isEmpty()) {
                0f
            } else {
                workouts.map { it.averageQuality }.average().toFloat()
            }
        }
    }

    override fun getTodayTotalDuration(): Flow<Long> {
        return getTodayWorkouts().map { workouts ->
            workouts.sumOf { it.durationMs }
        }
    }

    override fun getTodayWorkoutCountByType(): Flow<Map<String, Int>> {
        return getTodayWorkouts().map { workouts ->
            workouts.groupingBy { it.exerciseType }.eachCount()
        }
    }

    override fun getWeeklyStats(): Flow<WorkoutStats> {
        return getWeekWorkouts().map { workouts ->
            if (workouts.isEmpty()) {
                WorkoutStats()
            } else {
                WorkoutStats(
                    totalWorkouts = workouts.size,
                    totalReps = workouts.sumOf { it.totalReps },
                    totalDuration = workouts.sumOf { it.durationMs },
                    averageQuality = workouts.map { it.averageQuality }.average().toFloat(),
                    workoutsByType = workouts.groupingBy { it.exerciseType }.eachCount(),
                    bestGrade = workouts.minByOrNull { gradeToScore(it.grade) }?.grade,
                    averageScore = workouts.map { it.overallScore }.average().toFloat()
                )
            }
        }
    }

    /**
     * Convert grade to numeric score for comparison (lower is better)
     * A=1, B=2, C=3, D=4, F=5
     */
    private fun gradeToScore(grade: String): Int {
        return when (grade.uppercase()) {
            "A" -> 1
            "B" -> 2
            "C" -> 3
            "D" -> 4
            "F" -> 5
            else -> 6
        }
    }
}
