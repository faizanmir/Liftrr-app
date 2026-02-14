package org.liftrr.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.liftrr.data.local.WorkoutDao
import org.liftrr.data.models.WorkoutSessionEntity
import org.liftrr.utils.DispatcherProvider
import java.util.Calendar
import javax.inject.Inject

interface WorkoutRepository {
    fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>>
    fun getRecentWorkouts(limit: Int = 10): Flow<List<WorkoutSessionEntity>>
    fun getTodayWorkouts(): Flow<List<WorkoutSessionEntity>>
    fun getWorkoutsSince(startTime: Long): Flow<List<WorkoutSessionEntity>>
    fun getWeekWorkouts(): Flow<List<WorkoutSessionEntity>>
    fun getMonthWorkouts(): Flow<List<WorkoutSessionEntity>>
    suspend fun getWorkoutById(sessionId: String): WorkoutSessionEntity?
    suspend fun saveWorkout(workout: WorkoutSessionEntity)
    suspend fun markWorkoutAsDeleted(sessionId: String, deletedAt: Long = System.currentTimeMillis())
    suspend fun restoreWorkout(sessionId: String)
    suspend fun getDeletedWorkouts(): List<WorkoutSessionEntity>
    suspend fun permanentlyDeleteWorkout(sessionId: String)
    suspend fun permanentlyDeleteAllMarkedWorkouts()
    suspend fun deleteWorkout(workout: WorkoutSessionEntity)
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

class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val dispatchers: DispatcherProvider
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

    override suspend fun getWorkoutById(sessionId: String): WorkoutSessionEntity? = withContext(dispatchers.io) {
        workoutDao.getWorkoutById(sessionId)
    }

    override suspend fun saveWorkout(workout: WorkoutSessionEntity) = withContext(dispatchers.io) {
        workoutDao.insertWorkout(workout)
    }

    override suspend fun markWorkoutAsDeleted(sessionId: String, deletedAt: Long) = withContext(dispatchers.io) {
        workoutDao.markAsDeleted(sessionId, deletedAt)
    }

    override suspend fun restoreWorkout(sessionId: String) = withContext(dispatchers.io) {
        workoutDao.unmarkAsDeleted(sessionId)
    }

    override suspend fun getDeletedWorkouts(): List<WorkoutSessionEntity> = withContext(dispatchers.io) {
        workoutDao.getDeletedWorkouts()
    }

    override suspend fun permanentlyDeleteWorkout(sessionId: String) = withContext(dispatchers.io) {
        workoutDao.permanentlyDeleteWorkout(sessionId)
    }

    override suspend fun permanentlyDeleteAllMarkedWorkouts() = withContext(dispatchers.io) {
        workoutDao.permanentlyDeleteAllMarkedWorkouts()
    }

    override suspend fun deleteWorkout(workout: WorkoutSessionEntity) = withContext(dispatchers.io) {
        workoutDao.deleteWorkout(workout)
    }

    override suspend fun deleteAllWorkouts() = withContext(dispatchers.io) {
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
