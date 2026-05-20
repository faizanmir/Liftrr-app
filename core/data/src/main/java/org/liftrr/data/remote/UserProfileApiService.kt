package org.liftrr.data.remote

import org.liftrr.data.remote.dto.profile.UserProfileRequest
import org.liftrr.data.remote.dto.profile.UserProfileResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface UserProfileApiService {
    companion object {
        private const val API_PREFIX = "/api/v1"
    }

    @POST("${API_PREFIX}/profile")
    suspend fun createProfile(@Body request: UserProfileRequest): UserProfileResponse

    @GET("${API_PREFIX}/profile")
    suspend fun getProfile(): UserProfileResponse

    @PATCH("${API_PREFIX}/profile")
    suspend fun updateProfile(@Body request: UserProfileRequest): UserProfileResponse

    @DELETE("${API_PREFIX}/profile")
    suspend fun deleteProfile()
}
