package org.liftrr.data.remote

import org.liftrr.data.remote.dto.workout.ConfirmWorkoutVideoUploadRequest
import org.liftrr.data.remote.dto.workout.WorkoutSessionResponse
import org.liftrr.data.remote.dto.workout.WorkoutVideoUploadUrlRequest
import org.liftrr.data.remote.dto.workout.WorkoutVideoUploadUrlResponse
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST

interface WorkoutVideoApiService {
    companion object {
        private const val API_PREFIX = "/api/v1"
    }

    @POST("${API_PREFIX}/workout/video/upload-url")
    suspend fun requestUploadUrl(
        @Body request: WorkoutVideoUploadUrlRequest
    ): WorkoutVideoUploadUrlResponse

    @PATCH("${API_PREFIX}/workout/video")
    suspend fun confirmUpload(
        @Body request: ConfirmWorkoutVideoUploadRequest
    ): WorkoutSessionResponse
}
