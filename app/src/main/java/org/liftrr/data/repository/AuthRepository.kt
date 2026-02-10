package org.liftrr.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.liftrr.data.models.AuthResult
import org.liftrr.data.models.UserDto

/**
 * Authentication Repository Interface
 * This abstraction allows switching between local and remote authentication
 */
interface AuthRepository {
    /**
     * Sign in with email and password
     * @return AuthResult with user data or error
     */
    suspend fun signInWithEmailPassword(email: String, password: String): AuthResult

    /**
     * Sign up with email and password
     * @return AuthResult with user data or error
     */
    suspend fun signUpWithEmailPassword(
        email: String,
        password: String,
        firstName: String?,
        lastName: String?
    ): AuthResult

    /**
     * Sign in with Google
     * Handles the entire Google Sign-In flow internally
     * @param context Activity context required for displaying credential picker
     * @return AuthResult with user data or error
     */
    suspend fun signInWithGoogle(context: Context): AuthResult

    /**
     * Get currently logged in user
     * @return Flow of current user or null
     */
    fun getCurrentUser(): Flow<UserDto?>

    /**
     * Get current user once (suspend function)
     * @return Current user or null
     */
    suspend fun getCurrentUserOnce(): UserDto?

    /**
     * Sign out current user
     */
    suspend fun signOut()

    /**
     * Check if user is signed in
     * @return true if user is signed in
     */
    suspend fun isUserSignedIn(): Boolean
}
