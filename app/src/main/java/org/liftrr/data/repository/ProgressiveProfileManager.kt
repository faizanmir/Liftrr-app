package org.liftrr.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.liftrr.data.local.UserDao
import org.liftrr.data.local.UserPromptDao
import org.liftrr.data.local.WorkoutDao
import org.liftrr.data.models.ProfileCompleteness
import org.liftrr.data.models.PromptRules
import org.liftrr.data.models.PromptType
import org.liftrr.data.models.UserPromptEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages progressive profiling - determines when to show prompts and tracks completion
 *
 * Principles:
 * - Never annoy users with repeated prompts
 * - Show prompts at natural moments (e.g., after completing workouts)
 * - Allow users to dismiss prompts and respect their choice
 * - Track completion to avoid re-asking
 */
@Singleton
class ProgressiveProfileManager @Inject constructor(
    private val userDao: UserDao,
    private val userPromptDao: UserPromptDao,
    private val workoutDao: WorkoutDao
) {

    /**
     * Check if a specific prompt should be shown to the user
     * Returns true only if all conditions are met:
     * - User has done enough workouts
     * - Profile field is not already filled
     * - Prompt hasn't been completed or permanently dismissed
     * - Hasn't been shown too many times
     * - Enough time has passed since last dismissal
     */
    suspend fun shouldShowPrompt(userId: String, promptType: PromptType): Boolean {
        val user = userDao.getUserById(userId) ?: return false
        val workoutCount = workoutDao.getWorkoutCount(userId)
        val promptHistory = userPromptDao.getPrompt(userId, promptType)
        val profileData = ProfileCompleteness.from(user)

        return PromptRules.shouldShow(
            promptType = promptType,
            workoutCount = workoutCount,
            profileData = profileData,
            promptHistory = promptHistory
        )
    }

    /**
     * Get the next prompt that should be shown to the user (if any)
     * Returns null if no prompts should be shown right now
     *
     * Priority order:
     * 1. FITNESS_LEVEL (most important, after 1 workout)
     * 2. GOALS (after 3 workouts)
     * 3. NOTIFICATIONS (after 2 workouts, one-time)
     * 4. PROFILE_PHOTO (after 3 workouts)
     * 5. BODY_STATS (after 5 workouts)
     * 6. REMINDER_TIME (after 5 workouts)
     * 7. PREFERRED_EXERCISES (after 7 workouts)
     * 8. COMPLETE_PROFILE (after 10 workouts if < 70% complete)
     */
    suspend fun getNextPromptToShow(userId: String): PromptType? {
        val priorityOrder = listOf(
            PromptType.FITNESS_LEVEL,
            PromptType.GOALS,
            PromptType.NOTIFICATIONS,
            PromptType.PROFILE_PHOTO,
            PromptType.BODY_STATS,
            PromptType.REMINDER_TIME,
            PromptType.PREFERRED_EXERCISES,
            PromptType.COMPLETE_PROFILE
        )

        for (promptType in priorityOrder) {
            if (shouldShowPrompt(userId, promptType)) {
                return promptType
            }
        }

        return null
    }

    /**
     * Record that a prompt was shown to the user
     * Updates timestamp and increments counter
     */
    suspend fun trackPromptShown(userId: String, promptType: PromptType) {
        userPromptDao.trackPromptShown(userId, promptType)
    }

    /**
     * Mark a prompt as completed (user filled in the data)
     * This prevents the prompt from showing again
     */
    suspend fun markPromptCompleted(userId: String, promptType: PromptType) {
        userPromptDao.markCompleted(userId, promptType)
    }

    /**
     * Mark a prompt as dismissed (user clicked "Later" or "Skip")
     * Prompt may show again after DAYS_BEFORE_RESHOWING days
     */
    suspend fun markPromptDismissed(userId: String, promptType: PromptType) {
        userPromptDao.markDismissed(userId, promptType)
    }

    /**
     * Mark a prompt as permanently dismissed (user clicked "Don't ask again")
     * Prompt will never show again
     */
    suspend fun markPromptPermanentlyDismissed(userId: String, promptType: PromptType) {
        userPromptDao.markPermanentlyDismissed(userId, promptType)
    }

    /**
     * Get profile completeness for a user
     * Returns 0.0 to 1.0 (0% to 100%)
     */
    suspend fun getProfileCompleteness(userId: String): ProfileCompleteness {
        val user = userDao.getUserById(userId) ?: return ProfileCompleteness()
        return ProfileCompleteness.from(user)
    }

    /**
     * Observe profile completeness changes
     */
    fun observeProfileCompleteness(userId: String): Flow<ProfileCompleteness> {
        return userDao.observeUser(userId)
            .combine(userPromptDao.observePrompts(userId)) { user, _ ->
                user?.let { ProfileCompleteness.from(it) } ?: ProfileCompleteness()
            }
    }

    /**
     * Get prompt history for a user
     */
    suspend fun getPromptHistory(userId: String): List<UserPromptEntity> {
        return userPromptDao.getAllPrompts(userId)
    }

    /**
     * Check if a specific prompt has been completed
     */
    suspend fun isPromptCompleted(userId: String, promptType: PromptType): Boolean {
        return userPromptDao.isCompleted(userId, promptType) == true
    }

    /**
     * Get count of completed prompts
     */
    suspend fun getCompletedPromptCount(userId: String): Int {
        return userPromptDao.getCompletedCount(userId)
    }
}
