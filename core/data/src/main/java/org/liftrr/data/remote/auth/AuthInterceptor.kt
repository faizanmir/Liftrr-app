package org.liftrr.data.remote.auth

import okhttp3.Interceptor
import okhttp3.Response
import org.liftrr.data.local.preferences.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = tokenStore.getAccessToken()
        val request = if (accessToken.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        }
        return chain.proceed(request)
    }
}
