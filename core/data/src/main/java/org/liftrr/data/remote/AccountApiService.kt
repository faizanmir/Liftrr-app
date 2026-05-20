package org.liftrr.data.remote

import org.liftrr.data.remote.dto.auth.CurrentUserResponse
import retrofit2.http.GET

interface AccountApiService {
    companion object {
        private const val API_PREFIX = "/api/v1"
    }

    @GET("${API_PREFIX}/auth/me")
    suspend fun currentUser(): CurrentUserResponse
}
