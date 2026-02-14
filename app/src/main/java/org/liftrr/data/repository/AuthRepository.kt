package org.liftrr.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.liftrr.data.models.AuthResult
import org.liftrr.data.models.UserDto

interface AuthRepository {
    suspend fun signInWithEmailPassword(email: String, password: String): AuthResult

    suspend fun signUpWithEmailPassword(
        email: String,
        password: String,
        firstName: String?,
        lastName: String?
    ): AuthResult

    suspend fun signInWithGoogle(context: Context): AuthResult

    fun getCurrentUser(): Flow<UserDto?>

    suspend fun getCurrentUserOnce(): UserDto?

    suspend fun signOut()

    suspend fun isUserSignedIn(): Boolean
}
