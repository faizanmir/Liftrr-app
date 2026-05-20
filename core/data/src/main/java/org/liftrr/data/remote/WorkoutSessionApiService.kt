package org.liftrr.data.remote

import org.liftrr.data.remote.dto.workout.CreateWorkoutSessionRequest
import org.liftrr.data.remote.dto.workout.WorkoutSessionResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WorkoutSessionApiService {
    companion object {
        private const val API_PREFIX = "/api/v1"
    }

    @POST("${API_PREFIX}/workout/session")
    suspend fun createSession(
        @Body request: CreateWorkoutSessionRequest
    ): WorkoutSessionResponse

    @GET("${API_PREFIX}/workout/session")
    suspend fun listSessions(): List<WorkoutSessionResponse>

    @DELETE("${API_PREFIX}/workout/session/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: String)
}
