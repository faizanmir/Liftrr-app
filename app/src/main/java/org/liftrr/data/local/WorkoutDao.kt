package org.liftrr.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.liftrr.data.models.WorkoutSessionEntity

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentWorkouts(limit: Int = 10): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE isDeleted = 0 AND DATE(timestamp/1000, 'unixepoch') = DATE('now') ORDER BY timestamp DESC")
    fun getTodayWorkouts(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE isDeleted = 0 AND timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getTodayWorkoutsOptimized(startOfDay: Long, endOfDay: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE isDeleted = 0 AND timestamp >= :startTime ORDER BY timestamp DESC")
    fun getWorkoutsSince(startTime: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE isDeleted = 0 AND timestamp >= :startTime ORDER BY timestamp DESC LIMIT :limit")
    fun getWorkoutsSinceWithLimit(startTime: Long, limit: Int = 7): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getWorkoutById(sessionId: String): WorkoutSessionEntity?

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE userId = :userId AND isDeleted = 0")
    suspend fun getWorkoutCount(userId: String): Int

    @Query("SELECT * FROM workout_sessions WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    suspend fun getDeletedWorkouts(): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutSessionEntity)

    @Update
    suspend fun updateWorkout(workout: WorkoutSessionEntity)

    @Query("UPDATE workout_sessions SET isDeleted = 1, deletedAt = :deletedAt WHERE sessionId = :sessionId")
    suspend fun markAsDeleted(sessionId: String, deletedAt: Long)

    @Query("UPDATE workout_sessions SET isDeleted = 0, deletedAt = NULL WHERE sessionId = :sessionId")
    suspend fun unmarkAsDeleted(sessionId: String)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun permanentlyDeleteWorkout(sessionId: String)

    @Query("DELETE FROM workout_sessions WHERE isDeleted = 1")
    suspend fun permanentlyDeleteAllMarkedWorkouts()

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllWorkouts()
}
