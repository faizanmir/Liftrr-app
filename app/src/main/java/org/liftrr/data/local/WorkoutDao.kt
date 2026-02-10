package org.liftrr.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.liftrr.data.models.WorkoutSessionEntity

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentWorkouts(limit: Int = 10): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE DATE(timestamp/1000, 'unixepoch') = DATE('now') ORDER BY timestamp DESC")
    fun getTodayWorkouts(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getTodayWorkoutsOptimized(startOfDay: Long, endOfDay: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getWorkoutsSince(startTime: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE timestamp >= :startTime ORDER BY timestamp DESC LIMIT :limit")
    fun getWorkoutsSinceWithLimit(startTime: Long, limit: Int = 7): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getWorkoutById(sessionId: String): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutSessionEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllWorkouts()
}
