package org.liftrr.data.workmanager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.liftrr.data.local.LiftrrDb
import org.liftrr.data.local.workout.WorkoutDao
import org.liftrr.data.local.workout.WorkoutSyncQueueDao
import org.liftrr.data.local.SyncStatus
import org.liftrr.data.local.workout.WorkoutSessionEntity
import org.liftrr.data.local.workout.WorkoutSyncOperation
import org.liftrr.data.local.workout.WorkoutSyncQueueEntity
import org.liftrr.data.local.workout.WorkoutSyncState
import org.liftrr.data.remote.WorkoutSessionApiService
import org.liftrr.data.remote.WorkoutVideoApiService
import org.liftrr.data.remote.dto.workout.ConfirmWorkoutVideoUploadRequest
import org.liftrr.data.remote.dto.workout.WorkoutSessionResponse
import org.liftrr.data.remote.dto.workout.WorkoutVideoUploadUrlRequest
import org.liftrr.data.remote.dto.workout.toCreateWorkoutSessionRequest
import org.liftrr.utils.DispatcherProvider
import retrofit2.HttpException
import javax.inject.Named

@HiltWorker
class WorkoutSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: LiftrrDb,
    private val workoutDao: WorkoutDao,
    private val syncQueueDao: WorkoutSyncQueueDao,
    private val workoutSessionApiService: WorkoutSessionApiService,
    private val workoutVideoApiService: WorkoutVideoApiService,
    @param:Named("UnauthenticatedOkHttp")
    private val okHttpClient: OkHttpClient,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(dispatchers.io) {
        var shouldRetry = false

        do {
            val dueItems = syncQueueDao.getDueQueueItems(System.currentTimeMillis())
            dueItems.forEach { item ->
                syncQueueDao.updateState(
                    queueId = item.queueId,
                    state = WorkoutSyncState.RUNNING,
                    updatedAt = System.currentTimeMillis()
                )

                val itemResult = runCatching {
                    when (item.operation) {
                        WorkoutSyncOperation.UPSERT_WORKOUT -> syncUpsert(item)
                        WorkoutSyncOperation.DELETE_WORKOUT -> syncDelete(item)
                    }
                }

                if (itemResult.isSuccess) {
                    syncQueueDao.updateState(
                        queueId = item.queueId,
                        state = WorkoutSyncState.SUCCEEDED,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    shouldRetry = true
                    markFailed(item, itemResult.exceptionOrNull())
                }
            }
        } while (dueItems.isNotEmpty())

        if (shouldRetry) Result.retry() else Result.success()
    }

    private suspend fun syncUpsert(item: WorkoutSyncQueueEntity) {
        val workout = workoutDao.getWorkoutById(item.sessionId) ?: return
        if (workout.isDeleted) return

        workoutDao.updateSyncStatus(workout.sessionId, SyncStatus.SYNCING)

        val createResponse = workoutSessionApiService.createSession(
            workout.toCreateWorkoutSessionRequest()
        )
        val serverId = createResponse.backendSessionId
            ?: error("Backend did not return a workout session id")

        workoutDao.updateRemoteIdentity(
            sessionId = workout.sessionId,
            serverId = serverId,
            videoCloudUrl = createResponse.videoCloudUrl,
            version = createResponse.version
        )

        val afterCreate = workout.copy(
            serverId = serverId,
            videoCloudUrl = createResponse.videoCloudUrl ?: workout.videoCloudUrl,
            version = createResponse.version ?: workout.version
        )

        val finalResponse = uploadVideoIfNeeded(afterCreate)

        val syncedAt = System.currentTimeMillis()
        database.withTransaction {
            workoutDao.markSynced(
                sessionId = workout.sessionId,
                serverId = serverId,
                videoCloudUrl = finalResponse?.videoCloudUrl ?: afterCreate.videoCloudUrl,
                lastSyncedAt = finalResponse?.lastSyncedAt ?: syncedAt,
                version = finalResponse?.version ?: afterCreate.version
            )
        }
    }

    private suspend fun syncDelete(item: WorkoutSyncQueueEntity) {
        val workout = workoutDao.getWorkoutById(item.sessionId)

        if (workout?.serverId != null) {
            try {
                workoutSessionApiService.deleteSession(workout.serverId)
            } catch (e: HttpException) {
                if (e.code() != 404) throw e
            }
        }

        val now = System.currentTimeMillis()
        database.withTransaction {
            syncQueueDao.markOperationSucceeded(
                sessionId = item.sessionId,
                operation = WorkoutSyncOperation.UPSERT_WORKOUT,
                updatedAt = now
            )
            workoutDao.updateSyncStatus(item.sessionId, SyncStatus.SYNCED)
        }
    }

    private suspend fun uploadVideoIfNeeded(workout: WorkoutSessionEntity): WorkoutSessionResponse? {
        if (!workout.videoCloudUrl.isNullOrBlank()) return null
        val videoPath = workout.videoUri ?: return null
        val videoFile = File(videoPath)
        if (!videoFile.exists()) {
            Log.w(TAG, "Skipping video upload because file is missing: $videoPath")
            return null
        }
        val serverId = workout.serverId ?: error("Cannot upload video before server id is known")

        val uploadUrlResponse = workoutVideoApiService.requestUploadUrl(
            WorkoutVideoUploadUrlRequest(
                sessionId = serverId,
                clientSessionId = workout.sessionId,
                contentType = VIDEO_CONTENT_TYPE,
                fileName = videoFile.name
            )
        )
        val signedContentType = uploadUrlResponse.contentType.ifBlank { VIDEO_CONTENT_TYPE }
        val uploadRequest = Request.Builder()
            .url(uploadUrlResponse.uploadUrl)
            .put(videoFile.asRequestBody(signedContentType.toMediaType()))
            .build()

        okHttpClient.newCall(uploadRequest).execute().use { response ->
            if (!response.isSuccessful) {
                error("Video upload failed with HTTP ${response.code}")
            }
        }

        return workoutVideoApiService.confirmUpload(
            ConfirmWorkoutVideoUploadRequest(
                sessionId = serverId,
                clientSessionId = workout.sessionId,
                videoCloudUrl = uploadUrlResponse.videoCloudUrl
            )
        )
    }

    private suspend fun markFailed(item: WorkoutSyncQueueEntity, throwable: Throwable?) {
        val attemptCount = item.attemptCount + 1
        val now = System.currentTimeMillis()
        val nextEligibleAt = now + retryDelayMillis(attemptCount)
        val errorMessage = throwable?.message ?: throwable?.javaClass?.simpleName ?: "Unknown error"

        syncQueueDao.markFailed(
            queueId = item.queueId,
            state = WorkoutSyncState.FAILED,
            attemptCount = attemptCount,
            lastError = errorMessage.take(MAX_ERROR_LENGTH),
            updatedAt = now,
            nextEligibleAt = nextEligibleAt
        )

        if (item.operation == WorkoutSyncOperation.UPSERT_WORKOUT) {
            workoutDao.updateSyncStatus(item.sessionId, SyncStatus.FAILED)
        }

        Log.w(TAG, "Workout sync failed for ${item.queueId}: $errorMessage", throwable)
    }

    private fun retryDelayMillis(attemptCount: Int): Long {
        val exponent = (attemptCount - 1).coerceAtLeast(0).coerceAtMost(MAX_RETRY_EXPONENT)
        return BASE_RETRY_DELAY_MS * (1 shl exponent)
    }

    companion object {
        private const val TAG = "WorkoutSyncWorker"
        private const val VIDEO_CONTENT_TYPE = "video/mp4"
        private const val BASE_RETRY_DELAY_MS = 30_000L
        private const val MAX_RETRY_EXPONENT = 6
        private const val MAX_ERROR_LENGTH = 500
    }
}
