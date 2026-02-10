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
 * Onboarding Preferences for LIFTRR
 *
 * Tracks whether the user has completed the onboarding flow.
 * After onboarding is complete, the app will open directly to the home screen.
 *
 * Usage:
 * ```
 * val onboardingPrefs = OnboardingPreferences(context)
 * val isComplete by onboardingPrefs.isOnboardingComplete.collectAsState(initial = false)
 *
 * // Mark onboarding as complete
 * onboardingPrefs.setOnboardingComplete()
 * ```
 */

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_preferences")

class OnboardingPreferences(private val context: Context) {

    companion object {
        private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    /**
     * Flow of whether onboarding has been completed
     * Default: false (show onboarding)
     */
    val isOnboardingComplete: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETE] ?: false
        }

    /**
     * Mark onboarding as complete
     * User will skip onboarding screens on next app launch
     */
    suspend fun setOnboardingComplete() {
        context.onboardingDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETE] = true
        }
    }

    /**
     * Reset onboarding status (for testing or after logout)
     */
    suspend fun resetOnboarding() {
        context.onboardingDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETE] = false
        }
    }

    /**
     * Check if onboarding is complete (suspend function)
     *
     * @return true if onboarding completed, false otherwise
     */
    suspend fun isComplete(): Boolean {
        return isOnboardingComplete.first()
    }
}
