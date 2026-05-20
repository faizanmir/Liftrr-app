package org.liftrr.data.remote.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.liftrr.data.local.preferences.TokenStore
import org.liftrr.data.remote.AuthApiService
import org.liftrr.data.remote.dto.auth.RefreshRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val authApiService: AuthApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_RETRY_COUNT) return null

        val refreshToken = tokenStore.getRefreshToken() ?: return null
        val newTokens = runBlocking {
            runCatching { authApiService.refresh(RefreshRequest(refreshToken)) }.getOrNull()
        } ?: run {
            tokenStore.clearTokens()
            return null
        }

        tokenStore.saveTokens(newTokens.accessToken, newTokens.refreshToken)
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_RETRY_COUNT = 2
    }
}
