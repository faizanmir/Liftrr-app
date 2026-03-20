package org.liftrr.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.liftrr.data.local.preferences.TokenStore
import org.liftrr.domain.auth.AuthRepository
import org.liftrr.domain.auth.AuthResult
import org.liftrr.data.local.user.UserDao
import org.liftrr.data.models.dto.UserDto
import org.liftrr.data.remote.AuthApiService
import org.liftrr.data.remote.dto.auth.GoogleAuthRequest
import org.liftrr.data.remote.dto.auth.LoginRequest
import org.liftrr.data.remote.dto.auth.RefreshRequest
import org.liftrr.data.remote.dto.auth.RegisterRequest
import org.liftrr.data.repository.mappers.toDomain
import org.liftrr.data.services.GoogleAuthService
import org.liftrr.domain.user.AuthProvider
import org.liftrr.domain.user.User
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenStore: TokenStore,
    private val userDao: UserDao,
    private val googleAuthService: GoogleAuthService
) : AuthRepository {

    private companion object {
        const val TAG = "RemoteAuthRepository"
        const val ERR_GOOGLE = "Google sign-in failed"
    }

    override suspend fun signInWithEmailPassword(email: String, password: String): AuthResult {
        return try {
            val response = authApiService.login(LoginRequest(email, password))
            tokenStore.saveTokens(response.accessToken, response.refreshToken)

            val userId = decodeJwtSubject(response.accessToken) ?: UUID.randomUUID().toString()
            val existing = userDao.getUserByEmail(email)
            val user = existing?.copy(lastLoginAt = System.currentTimeMillis())
                ?: UserDto(
                    userId = userId,
                    email = email,
                    firstName = null,
                    lastName = null,
                    authProvider = AuthProvider.EMAIL_PASSWORD
                )
            if (existing != null) userDao.update(user) else userDao.insert(user)

            AuthResult.Success(user.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            AuthResult.Error(e.message ?: "Login failed")
        }
    }

    override suspend fun signUpWithEmailPassword(
        email: String,
        password: String,
        firstName: String?,
        lastName: String?
    ): AuthResult {
        return try {
            val response = authApiService.register(RegisterRequest(email, password))
            tokenStore.saveTokens(response.accessToken, response.refreshToken)

            val userId = decodeJwtSubject(response.accessToken) ?: UUID.randomUUID().toString()
            val user = UserDto(
                userId = userId,
                email = email,
                firstName = null,
                lastName = null,
                authProvider = AuthProvider.EMAIL_PASSWORD
            )
            userDao.insert(user)

            AuthResult.Success(user.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            AuthResult.Error(e.message ?: "Registration failed")
        }
    }

    override suspend fun signInWithGoogle(context: Context): AuthResult {
        return try {
            // 1. Full Firebase Google sign-in — same flow as before
            val firebaseUser = googleAuthService.signInWithGoogle(context).getOrElse {
                return AuthResult.Error(it.message ?: ERR_GOOGLE)
            }

            // 2. Get Firebase ID token and send to our backend
            val firebaseIdToken = firebaseUser.getIdToken(false).await().token
                ?: return AuthResult.Error("Failed to obtain Firebase ID token")

            val response = authApiService.googleAuth(GoogleAuthRequest(firebaseIdToken))
            tokenStore.saveTokens(response.accessToken, response.refreshToken)

            // 3. Build UserDto from the FirebaseUser — Google gives us name/email/photo
            val userId = decodeJwtSubject(response.accessToken) ?: firebaseUser.uid
            val email = firebaseUser.email
                ?: return AuthResult.Error("No email associated with Google account")

            val displayNameParts = firebaseUser.displayName?.split(" ") ?: emptyList()
            val existing = userDao.getUserByEmail(email)
            val user = existing?.copy(lastLoginAt = System.currentTimeMillis())
                ?: UserDto(
                    userId = userId,
                    email = email,
                    firstName = displayNameParts.firstOrNull(),
                    lastName = displayNameParts.drop(1).joinToString(" ").takeIf { it.isNotEmpty() },
                    authProvider = AuthProvider.GOOGLE,
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
            if (existing != null) userDao.update(user) else userDao.insert(user)

            AuthResult.Success(user.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, ERR_GOOGLE, e)
            AuthResult.Error(e.message ?: ERR_GOOGLE)
        }
    }

    override fun getCurrentUser(): Flow<User?> =
        userDao.getAll().map { users -> users.maxByOrNull { u -> u.lastLoginAt }?.toDomain() }

    override suspend fun getCurrentUserOnce(): User? =
        userDao.getLastLoggedInUser()?.toDomain()

    override suspend fun signOut() {
        try {
            val refreshToken = tokenStore.getRefreshToken()
            if (refreshToken != null) {
                authApiService.logout(RefreshRequest(refreshToken))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Logout API call failed — clearing local data anyway", e)
        } finally {
            tokenStore.clearTokens()
            userDao.deleteAll()
        }
    }

    override suspend fun isUserSignedIn(): Boolean =
        tokenStore.getAccessToken() != null

    private fun decodeJwtSubject(token: String): String? {
        return try {
            val payload = token.split(".")[1]
            val decoded = Base64.decode(
                payload,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            @Suppress("UNCHECKED_CAST")
            (Gson().fromJson(String(decoded), Map::class.java) as Map<String, Any>)["sub"] as? String
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode JWT subject", e)
            null
        }
    }
}
