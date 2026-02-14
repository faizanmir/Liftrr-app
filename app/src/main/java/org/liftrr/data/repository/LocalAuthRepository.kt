package org.liftrr.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.liftrr.data.local.UserDao
import org.liftrr.data.models.AuthProvider
import org.liftrr.data.models.AuthResult
import org.liftrr.data.models.UserDto
import org.liftrr.data.services.GoogleAuthService
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class LocalAuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val googleAuthService: GoogleAuthService
) : AuthRepository {

    private companion object {
        const val TAG = "LocalAuthRepository"
    }

    override suspend fun signInWithEmailPassword(email: String, password: String): AuthResult {
        return try {
            Log.d(TAG, "Attempting sign in for email: $email")

            val user = userDao.getUserByEmail(email)

            if (user == null) {
                Log.e(TAG, "User not found with email: $email")
                return AuthResult.Error("User not found. Please sign up first.")
            }

            val hashedPassword = hashPassword(password)
            if (user.passwordHash != hashedPassword) {
                Log.e(TAG, "Invalid password for email: $email")
                return AuthResult.Error("Invalid password")
            }

            val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
            userDao.update(updatedUser)

            Log.d(TAG, "Sign in successful for: $email")
            AuthResult.Success(updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error", e)
            AuthResult.Error("Sign in failed: ${e.message}")
        }
    }

    override suspend fun signUpWithEmailPassword(
        email: String,
        password: String,
        firstName: String?,
        lastName: String?
    ): AuthResult {
        return try {
            Log.d(TAG, "Attempting sign up for email: $email")

            val existingUser = userDao.getUserByEmail(email)
            if (existingUser != null) {
                Log.e(TAG, "User already exists with email: $email")
                return AuthResult.Error("User with this email already exists")
            }

            val userId = UUID.randomUUID().toString()
            val hashedPassword = hashPassword(password)

            val newUser = UserDto(
                userId = userId,
                firstName = firstName,
                lastName = lastName,
                email = email,
                passwordHash = hashedPassword,
                authProvider = AuthProvider.EMAIL_PASSWORD,
                photoUrl = null,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )

            userDao.insert(newUser)

            Log.d(TAG, "Sign up successful for: $email")
            Log.d(TAG, "User created: $newUser")

            AuthResult.Success(newUser)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error", e)
            AuthResult.Error("Sign up failed: ${e.message}")
        }
    }

    override suspend fun signInWithGoogle(context: android.content.Context): AuthResult {
        return try {
            Log.d(TAG, "Initiating Google sign in")

            val result = googleAuthService.signInWithGoogle(context)

            result.fold(
                onSuccess = { firebaseUser ->
                    Log.d(TAG, "Processing Google sign in for: ${firebaseUser.email}")

                    val email = firebaseUser.email
                        ?: return AuthResult.Error("No email found in Google account")

                    var user = userDao.getUserByEmail(email)

                    if (user == null) {
                        val displayNameParts = firebaseUser.displayName?.split(" ") ?: listOf()
                        val firstName = displayNameParts.firstOrNull()
                        val lastName = displayNameParts.drop(1).joinToString(" ").takeIf { it.isNotEmpty() }

                        user = UserDto(
                            userId = firebaseUser.uid,
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            passwordHash = null,
                            authProvider = AuthProvider.GOOGLE,
                            photoUrl = firebaseUser.photoUrl?.toString(),
                            createdAt = System.currentTimeMillis(),
                            lastLoginAt = System.currentTimeMillis()
                        )

                        userDao.insert(user)
                        Log.d(TAG, "New Google user created: $user")
                    } else {
                        user = user.copy(lastLoginAt = System.currentTimeMillis())
                        userDao.update(user)
                        Log.d(TAG, "Existing Google user logged in: ${user.email}")
                    }

                    AuthResult.Success(user)
                },
                onFailure = { exception ->
                    Log.e(TAG, "Google sign in failed", exception)
                    AuthResult.Error(exception.message ?: "Google sign in failed")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in error", e)
            AuthResult.Error("Google sign in failed: ${e.message}")
        }
    }

    override fun getCurrentUser(): Flow<UserDto?> {
        return userDao.getAll().map { users ->
            users.maxByOrNull { it.lastLoginAt }
        }
    }

    override suspend fun getCurrentUserOnce(): UserDto? {
        return userDao.getLastLoggedInUser()
    }

    override suspend fun signOut() {
        Log.d(TAG, "Signing out user")
        try {
            userDao.deleteAll()
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            Log.d(TAG, "User signed out and data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out", e)
            throw e
        }
    }

    override suspend fun isUserSignedIn(): Boolean {
        val user = userDao.getLastLoggedInUser()
        return user != null
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(password.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
