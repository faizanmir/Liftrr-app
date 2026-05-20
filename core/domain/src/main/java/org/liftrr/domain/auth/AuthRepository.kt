package org.liftrr.domain.auth

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.liftrr.domain.user.User

interface AuthRepository {
    suspend fun signInWithEmailPassword(email: String, password: String): AuthResult
    suspend fun signUpWithEmailPassword(
        email: String,
        password: String,
        firstName: String?,
        lastName: String?
    ): AuthResult
    suspend fun signInWithGoogle(context: Context): AuthResult
    fun getCurrentUser(): Flow<User?>
    suspend fun getCurrentUserOnce(): User?
    suspend fun signOut()
    suspend fun isUserSignedIn(): Boolean
}
