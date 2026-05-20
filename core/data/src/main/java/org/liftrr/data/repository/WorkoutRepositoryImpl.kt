package org.liftrr.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.liftrr.data.local.LiftrrDb
import org.liftrr.data.local.preferences.TokenStore
import org.liftrr.data.local.workout.WorkoutDao
import org.liftrr.data.local.workout.WorkoutSyncQueueDao
import org.liftrr.data.local.SyncStatus
import org.liftrr.data.local.workout.WorkoutSessionEntity
import org.liftrr.data.local.workout.WorkoutSyncOperation
import org.liftrr.data.local.workout.WorkoutSyncQueueEntity
import org.liftrr.data.local.workout.WorkoutSyncState
import org.liftrr.data.remote.WorkoutSessionApiService
import org.liftrr.data.remote.dto.workout.WorkoutSessionResponse
import org.liftrr.data.repository.mappers.toDomain
import org.liftrr.data.repository.mappers.toEntity
import org.liftrr.data.workmanager.WorkoutSyncScheduler
import org.liftrr.domain.workout.WorkoutRecord
import org.liftrr.domain.workout.WorkoutRepository
import org.liftrr.domain.workout.WorkoutStats
import org.liftrr.utils.DispatcherProvider
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val database: LiftrrDb,
    private val workoutDao: WorkoutDao,
    private val syncQueueDao: WorkoutSyncQueueDao,
    private val tokenStore: TokenStore,
    private val syncScheduler: WorkoutSyncScheduler,
    private val dispatchers: DispatcherProvider,
    private val workoutSessionApiService: WorkoutSessionApiService
) : WorkoutRepository {

    override fun getAllWorkouts(): Flow<List<WorkoutRecord>> =
        workoutDao.getAllWorkouts().map { it.map { entity -> entity.toDomain() } }

    override fun getRecentWorkouts(limit: Int): Flow<List<WorkoutRecord>> =
        workoutDao.getRecentWorkouts(limit).map { it.map { entity -> entity.toDomain() } }

    override fun getTodayWorkouts(): Flow<List<WorkoutRecord>> =
        workoutDao.getTodayWorkouts().map { it.map { entity -> entity.toDomain() } }

    override fun getWorkoutsSince(startTime: Long): Flow<List<WorkoutRecord>> =
        workoutDao.getWorkoutsSince(startTime).map { it.map { entity -> entity.toDomain() } }

    override fun getWeekWorkouts(): Flow<List<WorkoutRecord>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        return workoutDao.getWorkoutsSinceWithLimit(calendar.timeInMillis, limit = 100)
            .map { it.map { entity -> entity.toDomain() } }
    }

    override fun getMonthWorkouts(): Flow<List<WorkoutRecord>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return workoutDao.getWorkoutsSince(calendar.timeInMillis)
            .map { it.map { entity -> entity.toDomain() } }
    }

    override suspend fun getWorkoutById(sessionId: String): WorkoutRecord? =
        withContext(dispatchers.io) {
            workoutDao.getWorkoutById(sessionId)?.toDomain()
        }

    override suspend fun saveWorkout(workout: WorkoutRecord) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val entity = workout.copy(userId = resolveUserId(workout)).toEntity(
            syncStatus = SyncStatus.PENDING
        )
        database.withTransaction {
            workoutDao.insertWorkout(entity)
            syncQueueDao.upsert(
                WorkoutSyncQueueEntity(
                    queueId = WorkoutSyncQueueEntity.queueId(
                        workout.sessionId,
                        WorkoutSyncOperation.UPSERT_WORKOUT
                    ),
                    sessionId = workout.sessionId,
                    operation = WorkoutSyncOperation.UPSERT_WORKOUT,
                    state = WorkoutSyncState.PENDING,
                    createdAt = now,
                    updatedAt = now,
                    nextEligibleAt = now
                )
            )
        }
        syncScheduler.scheduleSync()
    }

    override suspend fun markWorkoutAsDeleted(sessionId: String, deletedAt: Long) =
        withContext(dispatchers.io) {
            val now = System.currentTimeMillis()
            database.withTransaction {
                workoutDao.markAsDeleted(sessionId, deletedAt)
                syncQueueDao.upsert(
                    WorkoutSyncQueueEntity(
                        queueId = WorkoutSyncQueueEntity.queueId(
                            sessionId,
                            WorkoutSyncOperation.DELETE_WORKOUT
                        ),
                        sessionId = sessionId,
                        operation = WorkoutSyncOperation.DELETE_WORKOUT,
                        state = WorkoutSyncState.PENDING,
                        createdAt = now,
                        updatedAt = now,
                        nextEligibleAt = now
                    )
                )
            }
            syncScheduler.scheduleSync()
        }

    override suspend fun restoreWorkout(sessionId: String) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            workoutDao.unmarkAsDeleted(sessionId)
            syncQueueDao.markOperationSucceeded(
                sessionId = sessionId,
                operation = WorkoutSyncOperation.DELETE_WORKOUT,
                updatedAt = now
            )
        }
        syncScheduler.scheduleSync()
    }

    override suspend fun getDeletedWorkouts(): List<WorkoutRecord> = withContext(dispatchers.io) {
        workoutDao.getDeletedWorkouts().map { it.toDomain() }
    }

    override suspend fun permanentlyDeleteWorkout(sessionId: String) = withContext(dispatchers.io) {
        workoutDao.permanentlyDeleteWorkout(sessionId)
    }

    override suspend fun permanentlyDeleteAllMarkedWorkouts() = withContext(dispatchers.io) {
        workoutDao.permanentlyDeleteAllMarkedWorkouts()
    }

    override suspend fun deleteWorkout(workout: WorkoutRecord) = withContext(dispatchers.io) {
        workoutDao.deleteWorkout(workout.toEntity())
    }

    override suspend fun deleteAllWorkouts() = withContext(dispatchers.io) {
        workoutDao.deleteAllWorkouts()
    }

    override fun getTodayTotalReps(): Flow<Int> =
        getTodayWorkouts().map { workouts -> workouts.sumOf { it.totalReps } }

    override fun getTodayAverageQuality(): Flow<Float> =
        getTodayWorkouts().map { workouts ->
            if (workouts.isEmpty()) 0f
            else workouts.map { it.averageQuality }.average().toFloat()
        }

    override fun getTodayTotalDuration(): Flow<Long> =
        getTodayWorkouts().map { workouts -> workouts.sumOf { it.durationMs } }

    override fun getTodayWorkoutCountByType(): Flow<Map<String, Int>> =
        getTodayWorkouts().map { workouts ->
            workouts.groupingBy { it.exerciseType }.eachCount()
        }

    override fun getWeeklyStats(): Flow<WorkoutStats> =
        getWeekWorkouts().map { workouts ->
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

    private fun gradeToScore(grade: String): Int = when (grade.uppercase()) {
        "A" -> 1; "B" -> 2; "C" -> 3; "D" -> 4; "F" -> 5; else -> 6
    }

    override suspend fun syncFromRemote(): Result<Unit> = runCatching {
        withContext(dispatchers.io) {
            val sessions = workoutSessionApiService.listSessions()
            val userId = tokenStore.getUserIdFromAccessToken() ?: return@withContext
            sessions.forEach { response ->
                val serverId = response.backendSessionId ?: return@forEach
                val existing = workoutDao.getByServerId(serverId)
                if (existing == null) {
                    workoutDao.insertWorkout(response.toRemoteEntity(userId))
                }
            }
        }
    }

    private fun resolveUserId(workout: WorkoutRecord): String =
        tokenStore.getUserIdFromAccessToken()
            ?: workout.userId.takeUnless { it == "local" }
            ?: error("Cannot save workout without an authenticated backend user")
}

private fun WorkoutSessionResponse.toRemoteEntity(userId: String) = WorkoutSessionEntity(
    sessionId = UUID.randomUUID().toString(),
    serverId = backendSessionId,
    exerciseType = exerciseType ?: "",
    totalReps = totalReps ?: 0,
    goodReps = goodReps ?: 0,
    badReps = badReps ?: 0,
    averageQuality = averageQuality ?: 0f,
    durationMs = durationMs ?: 0L,
    overallScore = overallScore ?: 0f,
    grade = grade ?: "",
    weight = weight,
    timestamp = timestamp ?: 0L,
    videoUri = null,
    repDataJson = null,
    keyFramesJson = null,
    videoCloudUrl = videoCloudUrl,
    userId = userId,
    syncStatus = SyncStatus.SYNCED,
    lastSyncedAt = System.currentTimeMillis()
)
