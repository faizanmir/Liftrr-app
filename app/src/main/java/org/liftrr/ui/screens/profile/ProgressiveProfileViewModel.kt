package org.liftrr.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.liftrr.data.models.ProfileCompleteness
import org.liftrr.data.models.PromptType
import org.liftrr.data.repository.AuthRepository
import org.liftrr.data.repository.ProgressiveProfileManager
import javax.inject.Inject

/**
 * UI state for progressive profile prompts
 */
data class ProgressiveProfileState(
    val currentPrompt: PromptType? = null,
    val profileCompleteness: ProfileCompleteness = ProfileCompleteness(),
    val showPrompt: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for managing progressive profile prompts
 * Determines when to show prompts and handles user interactions
 */
@HiltViewModel
class ProgressiveProfileViewModel @Inject constructor(
    private val progressiveProfileManager: ProgressiveProfileManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressiveProfileState())
    val state: StateFlow<ProgressiveProfileState> = _state.asStateFlow()

    init {
        observeProfileCompleteness()
    }

    /**
     * Observe profile completeness changes
     */
    private fun observeProfileCompleteness() {
        viewModelScope.launch {
            authRepository.getCurrentUser()
                .filterNotNull()
                .flatMapLatest { user ->
                    progressiveProfileManager.observeProfileCompleteness(user.userId)
                }
                .collect { completeness ->
                    _state.update { it.copy(profileCompleteness = completeness) }
                }
        }
    }

    /**
     * Check if a prompt should be shown after workout completion
     * Call this after the user completes a workout
     */
    fun checkForPromptAfterWorkout() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserOnce()?.userId ?: return@launch

            val nextPrompt = progressiveProfileManager.getNextPromptToShow(userId)
            if (nextPrompt != null) {
                _state.update {
                    it.copy(
                        currentPrompt = nextPrompt,
                        showPrompt = true
                    )
                }
                // Track that we showed this prompt
                progressiveProfileManager.trackPromptShown(userId, nextPrompt)
            }
        }
    }

    /**
     * User completed the prompt (filled in the data)
     * Navigate to the appropriate screen to fill in the data
     */
    fun onPromptComplete(promptType: PromptType) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserOnce()?.userId ?: return@launch
            progressiveProfileManager.markPromptCompleted(userId, promptType)
            _state.update { it.copy(showPrompt = false, currentPrompt = null) }
        }
    }

    /**
     * User dismissed the prompt (clicked "Later")
     * Prompt may show again in the future
     */
    fun onPromptDismiss(promptType: PromptType) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserOnce()?.userId ?: return@launch
            progressiveProfileManager.markPromptDismissed(userId, promptType)
            _state.update { it.copy(showPrompt = false, currentPrompt = null) }
        }
    }

    /**
     * User permanently dismissed the prompt (clicked "Don't ask again")
     * Prompt will never show again
     */
    fun onPromptDismissForever(promptType: PromptType) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserOnce()?.userId ?: return@launch
            progressiveProfileManager.markPromptPermanentlyDismissed(userId, promptType)
            _state.update { it.copy(showPrompt = false, currentPrompt = null) }
        }
    }

    /**
     * Dismiss the current prompt (close the bottom sheet)
     */
    fun dismissPrompt() {
        _state.update { it.copy(showPrompt = false) }
    }

    /**
     * Manually trigger a check for prompts (e.g., from settings screen)
     * This bypasses workout count requirements and shows the first incomplete prompt
     */
    fun checkForPrompts() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserOnce()?.userId

            if (userId == null) {
                android.util.Log.w("ProgressiveProfileVM", "No user logged in")
                _state.update { it.copy(errorMessage = "Please sign in to complete your profile") }
                return@launch
            }

            // Get the first incomplete prompt, ignoring workout count requirements
            val nextPrompt = getNextIncompletePrompt(userId)

            if (nextPrompt != null) {
                android.util.Log.d("ProgressiveProfileVM", "Showing prompt: $nextPrompt")
                _state.update {
                    it.copy(
                        currentPrompt = nextPrompt,
                        showPrompt = true,
                        errorMessage = null
                    )
                }
                // Track that we showed this prompt
                progressiveProfileManager.trackPromptShown(userId, nextPrompt)
            } else {
                android.util.Log.d("ProgressiveProfileVM", "No incomplete prompts available")
                _state.update { it.copy(errorMessage = "Your profile is already complete!") }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Get the next incomplete prompt, ignoring workout count requirements
     * Used for manual profile completion
     */
    private suspend fun getNextIncompletePrompt(userId: String): PromptType? {
        val user = authRepository.getCurrentUserOnce() ?: return null
        val profileData = progressiveProfileManager.getProfileCompleteness(userId)

        // Priority order of prompts
        val priorityOrder = listOf(
            PromptType.FITNESS_LEVEL to !profileData.hasFitnessLevel,
            PromptType.GOALS to !profileData.hasGoals,
            PromptType.BODY_STATS to !profileData.hasBodyStats,
            PromptType.PREFERRED_EXERCISES to !profileData.hasPreferredExercises,
            PromptType.PROFILE_PHOTO to !profileData.hasProfilePhoto,
            PromptType.REMINDER_TIME to !profileData.hasReminderTime,
            PromptType.NOTIFICATIONS to false // Skip this one for manual completion
        )

        // Find the first incomplete prompt that hasn't been permanently dismissed
        for ((promptType, isIncomplete) in priorityOrder) {
            if (isIncomplete) {
                val promptHistory = progressiveProfileManager.getPromptHistory(userId)
                    .find { it.promptType == promptType }

                // Skip if permanently dismissed
                if (promptHistory?.shouldShowAgain == false) continue

                return promptType
            }
        }

        return null
    }

    /**
     * Get profile completeness percentage
     */
    fun getProfileCompletenessPercentage(): String {
        return state.value.profileCompleteness.percentageString
    }

    /**
     * Check if profile is considered complete (>= 70%)
     */
    fun isProfileComplete(): Boolean {
        return state.value.profileCompleteness.isComplete
    }
}
