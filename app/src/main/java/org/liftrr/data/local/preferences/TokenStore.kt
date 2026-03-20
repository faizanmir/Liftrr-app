package org.liftrr.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore by preferencesDataStore(name = "auth_tokens")

@Singleton
class TokenStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[accessTokenKey] = accessToken
            prefs[refreshTokenKey] = refreshToken
        }
    }

    suspend fun getAccessToken(): String? =
        context.tokenDataStore.data.map { it[accessTokenKey] }.first()

    suspend fun getRefreshToken(): String? =
        context.tokenDataStore.data.map { it[refreshTokenKey] }.first()

    suspend fun clearTokens() {
        context.tokenDataStore.edit { it.clear() }
    }
}
