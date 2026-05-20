package org.liftrr.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.liftrr.data.local.preferences.TokenStore
import org.liftrr.domain.auth.AuthRepository
import org.liftrr.domain.auth.AuthResult
import org.liftrr.data.remote.AccountApiService
import org.liftrr.data.remote.AuthApiService
import org.liftrr.data.remote.dto.auth.GoogleAuthRequest
import org.liftrr.data.remote.dto.auth.LoginRequest
import org.liftrr.data.remote.dto.auth.RefreshRequest
import org.liftrr.data.remote.dto.auth.RegisterRequest
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
    private val googleAuthService: GoogleAuthService,
    private val appScope: CoroutineScope
) : AuthRepository {

    private companion object {
        const val TAG = "RemoteAuthRepository"
        const val ERR_GOOGLE = "Google sign-in failed"
    }

    private val _currentUser = MutableStateFlow<User?>(null)

    init {
        if (tokenStore.getAccessToken() != null) {
            appScope.launch {
                runCatching { restoreSession() }.onFailure {
                    Log.w(TAG, "Session restore failed", it)
                }
            }
        }
    }

    private suspend fun restoreSession() {
        val response = accountApiService.currentUser()
        val nameParts = response.name?.split(" ") ?: emptyList()
        _currentUser.value = User(
            userId = response.userId,
            email = response.email,
            firstName = nameParts.firstOrNull(),
            lastName = nameParts.drop(1).joinToString(" ").takeIf { it.isNotEmpty() },
            authProvider = if (response.hasGoogleAuth) AuthProvider.GOOGLE else AuthProvider.EMAIL_PASSWORD
        )
    }

    override suspend fun signInWithEmailPassword(email: String, password: String): AuthResult {
        return try {
            val response = authApiService.login(LoginRequest(email, password))
            tokenStore.saveTokens(response.accessToken, response.refreshToken)
            val user = fetchAndSetCurrentUser(AuthProvider.EMAIL_PASSWORD)
            AuthResult.Success(user)
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
            val user = fetchAndSetCurrentUser(AuthProvider.EMAIL_PASSWORD)
            AuthResult.Success(user)
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
            val user = fetchAndSetCurrentUser(
                authProvider = AuthProvider.GOOGLE,
                fallbackName = googleToken.displayName,
                fallbackPhotoUrl = googleToken.photoUrl
            )
            AuthResult.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, ERR_GOOGLE, e)
            AuthResult.Error(e.message ?: ERR_GOOGLE)
        }
    }

    override fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override suspend fun getCurrentUserOnce(): User? = _currentUser.value

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
            _currentUser.value = null
        }
    }

    override suspend fun isUserSignedIn(): Boolean = tokenStore.getAccessToken() != null

    private suspend fun fetchAndSetCurrentUser(
        authProvider: AuthProvider,
        fallbackName: String? = null,
        fallbackPhotoUrl: String? = null
    ): User {
        val response = accountApiService.currentUser()
        val nameParts = (response.name ?: fallbackName)?.split(" ") ?: emptyList()
        val user = User(
            userId = response.userId,
            email = response.email,
            firstName = nameParts.firstOrNull(),
            lastName = nameParts.drop(1).joinToString(" ").takeIf { it.isNotEmpty() },
            authProvider = authProvider,
            photoUrl = fallbackPhotoUrl
        )
        _currentUser.value = user
        return user
    }
}
