package org.liftrr.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.liftrr.data.local.preferences.TokenStore
import org.liftrr.domain.auth.AuthRepository
import org.liftrr.domain.auth.AuthResult
import org.liftrr.data.local.user.UserDao
import org.liftrr.data.models.dto.UserDto
import org.liftrr.data.remote.AccountApiService
import org.liftrr.data.remote.AuthApiService
import org.liftrr.data.remote.dto.auth.GoogleAuthRequest
import org.liftrr.data.remote.dto.auth.LoginRequest
import org.liftrr.data.remote.dto.auth.RefreshRequest
import org.liftrr.data.remote.dto.auth.RegisterRequest
import org.liftrr.data.repository.mappers.toDomain
import org.liftrr.data.services.GoogleAuthService
import org.liftrr.domain.user.AuthProvider
import org.liftrr.domain.user.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val accountApiService: AccountApiService,
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

            val user = upsertCurrentUser(AuthProvider.EMAIL_PASSWORD)

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

            val user = upsertCurrentUser(AuthProvider.EMAIL_PASSWORD)

            AuthResult.Success(user.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            AuthResult.Error(e.message ?: "Registration failed")
        }
    }

    override suspend fun signInWithGoogle(context: Context): AuthResult {
        return try {
            val googleToken = googleAuthService.getGoogleSignInToken(context).getOrElse {
                return AuthResult.Error(it.message ?: ERR_GOOGLE)
            }

            val response = authApiService.googleAuth(GoogleAuthRequest(googleToken.idToken))
            tokenStore.saveTokens(response.accessToken, response.refreshToken)

            val user = upsertCurrentUser(
                authProvider = AuthProvider.GOOGLE,
                fallbackName = googleToken.displayName,
                fallbackPhotoUrl = googleToken.photoUrl
            )

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

    private suspend fun upsertCurrentUser(
        authProvider: AuthProvider,
        fallbackName: String? = null,
        fallbackPhotoUrl: String? = null
    ): UserDto {
        val currentUser = accountApiService.currentUser()
        val displayNameParts = (currentUser.name ?: fallbackName)?.split(" ") ?: emptyList()
        val existing = userDao.getUserByEmail(currentUser.email)
        val user = existing?.copy(
            userId = currentUser.userId,
            firstName = displayNameParts.firstOrNull() ?: existing.firstName,
            lastName = displayNameParts.drop(1).joinToString(" ").takeIf { it.isNotEmpty() } ?: existing.lastName,
            photoUrl = fallbackPhotoUrl ?: existing.photoUrl,
            lastLoginAt = System.currentTimeMillis()
        ) ?: UserDto(
            userId = currentUser.userId,
            email = currentUser.email,
            firstName = displayNameParts.firstOrNull(),
            lastName = displayNameParts.drop(1).joinToString(" ").takeIf { it.isNotEmpty() },
            authProvider = authProvider,
            photoUrl = fallbackPhotoUrl
        )

        if (existing != null) userDao.update(user) else userDao.insert(user)
        return user
    }
}
