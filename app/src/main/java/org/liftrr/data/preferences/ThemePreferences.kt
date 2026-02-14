package org.liftrr.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {

    companion object {
        private val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        private val USE_DARK_MODE = booleanPreferencesKey("use_dark_mode")
    }

    val useDynamicColor: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_DYNAMIC_COLOR] ?: false
        }

    val useDarkMode: Flow<Boolean?> = context.dataStore.data
        .map { preferences ->
            if (preferences.contains(USE_DARK_MODE)) {
                preferences[USE_DARK_MODE]
            } else {
                null
            }
        }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setDarkMode(enabled: Boolean?) {
        context.dataStore.edit { preferences ->
            if (enabled != null) {
                preferences[USE_DARK_MODE] = enabled
            } else {
                preferences.remove(USE_DARK_MODE)
            }
        }
    }

    suspend fun isDarkModeEnabled(): Boolean {
        return useDarkMode.first() ?: false
    }

    suspend fun isDynamicColorEnabled(): Boolean {
        return useDynamicColor.first()
    }
}