package org.liftrr.data.remote

import org.liftrr.data.remote.dto.auth.AuthResponse
import org.liftrr.data.remote.dto.auth.GoogleAuthRequest
import org.liftrr.data.remote.dto.auth.LoginRequest
import org.liftrr.data.remote.dto.auth.RefreshRequest
import org.liftrr.data.remote.dto.auth.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    companion object {
        private const val AUTH_PREFIX = "/api/v1"
    }

    @POST("${AUTH_PREFIX}/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("${AUTH_PREFIX}/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("${AUTH_PREFIX}/auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): AuthResponse

    @POST("${AUTH_PREFIX}/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse

    @POST("${AUTH_PREFIX}/auth/logout")
    suspend fun logout(@Body request: RefreshRequest)
}
