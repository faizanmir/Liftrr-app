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

/**
 * Theme Preferences for LIFTRR
 *
 * Manages user theme settings including:
 * - Dynamic color (Material You wallpaper colors) toggle
 * - Dark mode preferences
 *
 * Usage in settings screen:
 * ```
 * val themePrefs = ThemePreferences(context)
 * val useDynamicColor by themePrefs.useDynamicColor.collectAsState(initial = false)
 *
 * // Toggle switch
 * Switch(
 *     checked = useDynamicColor,
 *     onCheckedChange = { themePrefs.setDynamicColor(it) }
 * )
 * ```
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {

    companion object {
        private val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        private val USE_DARK_MODE = booleanPreferencesKey("use_dark_mode")
    }

    /**
     * Flow of whether to use dynamic colors from wallpaper
     * Default: false (use energetic fitness colors)
     */
    val useDynamicColor: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_DYNAMIC_COLOR] ?: false // Default to fitness colors
        }

    /**
     * Flow of whether to use dark mode
     * Default: null (follow system)
     */
    val useDarkMode: Flow<Boolean?> = context.dataStore.data
        .map { preferences ->
            if (preferences.contains(USE_DARK_MODE)) {
                preferences[USE_DARK_MODE]
            } else {
                null // Follow system setting
            }
        }

    /**
     * Enable or disable dynamic colors from wallpaper
     *
     * @param enabled true = use wallpaper colors, false = use fitness brand colors
     */
    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_DYNAMIC_COLOR] = enabled
        }
    }

    /**
     * Set dark mode preference
     *
     * @param enabled true = dark, false = light, null = follow system
     */
    suspend fun setDarkMode(enabled: Boolean?) {
        context.dataStore.edit { preferences ->
            if (enabled != null) {
                preferences[USE_DARK_MODE] = enabled
            } else {
                preferences.remove(USE_DARK_MODE)
            }
        }
    }

    /**
     * Get current dark mode setting
     *
     * @return true if dark mode enabled, false if light mode, null if following system
     */
    suspend fun isDarkModeEnabled(): Boolean {
        return useDarkMode.first() ?: false
    }

    /**
     * Get current dynamic color setting
     *
     * @return true if dynamic colors enabled, false if using fitness brand colors
     */
    suspend fun isDynamicColorEnabled(): Boolean {
        return useDynamicColor.first()
    }
}