package org.liftrr.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Types of progressive profile prompts shown to users
 */
enum class PromptType {
    FITNESS_LEVEL,          // Ask about experience level
    GOALS,                  // Ask about fitness goals
    BODY_STATS,            // Ask for height/weight
    PREFERRED_EXERCISES,   // Ask about exercise preferences
    NOTIFICATIONS,         // Ask about notification preferences
    REMINDER_TIME,         // Ask about workout reminders
    PROFILE_PHOTO,         // Prompt to add profile photo
    COMPLETE_PROFILE      // General "complete your profile" prompt
}

/**
 * Tracks which progressive profile prompts have been shown to the user
 * Prevents annoying users with repeated prompts
 */
@Entity(
    tableName = "user_prompts",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["promptType"]),
        Index(value = ["userId", "promptType"], unique = true)
    ]
)
data class UserPromptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String,                     // Which user this prompt is for
    val promptType: PromptType,             // Type of prompt shown

    val firstShownAt: Long,                 // When first shown
    val lastShownAt: Long,                  // When last shown
    val timesShown: Int = 1,                // How many times shown

    val completed: Boolean = false,         // Did user complete the prompt?
    val completedAt: Long? = null,          // When they completed it

    val dismissed: Boolean = false,         // Did user dismiss/skip it?
    val dismissedAt: Long? = null,          // When they dismissed it

    val shouldShowAgain: Boolean = true     // Should we show again? (false = permanently dismissed)
)

/**
 * Rules for when to show progressive prompts
 */
object PromptRules {
    // Minimum workouts before showing prompts
    const val MIN_WORKOUTS_FOR_FITNESS_LEVEL = 1
    const val MIN_WORKOUTS_FOR_GOALS = 3
    const val MIN_WORKOUTS_FOR_BODY_STATS = 5
    const val MIN_WORKOUTS_FOR_PREFERRED_EXERCISES = 7

    // Days between re-showing dismissed prompts
    const val DAYS_BEFORE_RESHOWING = 7

    // Max times to show a prompt before giving up
    const val MAX_TIMES_TO_SHOW = 3

    /**
     * Should we show this prompt type to the user now?
     */
    fun shouldShow(
        promptType: PromptType,
        workoutCount: Int,
        profileData: ProfileCompleteness,
        promptHistory: UserPromptEntity?
    ): Boolean {
        // Already completed? Don't show
        if (promptHistory?.completed == true) return false

        // Permanently dismissed? Don't show
        if (promptHistory?.shouldShowAgain == false) return false

        // Shown too many times? Don't show
        if (promptHistory != null && promptHistory.timesShown >= MAX_TIMES_TO_SHOW) return false

        // Recently dismissed? Don't show yet
        if (promptHistory?.dismissedAt != null) {
            val daysSinceDismissal = (System.currentTimeMillis() - promptHistory.dismissedAt) / (1000 * 60 * 60 * 24)
            if (daysSinceDismissal < DAYS_BEFORE_RESHOWING) return false
        }

        // Check specific conditions for each prompt type
        return when (promptType) {
            PromptType.FITNESS_LEVEL -> {
                workoutCount >= MIN_WORKOUTS_FOR_FITNESS_LEVEL && !profileData.hasFitnessLevel
            }
            PromptType.GOALS -> {
                workoutCount >= MIN_WORKOUTS_FOR_GOALS && !profileData.hasGoals
            }
            PromptType.BODY_STATS -> {
                workoutCount >= MIN_WORKOUTS_FOR_BODY_STATS && !profileData.hasBodyStats
            }
            PromptType.PREFERRED_EXERCISES -> {
                workoutCount >= MIN_WORKOUTS_FOR_PREFERRED_EXERCISES && !profileData.hasPreferredExercises
            }
            PromptType.NOTIFICATIONS -> {
                workoutCount >= 2 && promptHistory == null // Show once early on
            }
            PromptType.REMINDER_TIME -> {
                workoutCount >= 5 && !profileData.hasReminderTime
            }
            PromptType.PROFILE_PHOTO -> {
                workoutCount >= 3 && !profileData.hasProfilePhoto
            }
            PromptType.COMPLETE_PROFILE -> {
                workoutCount >= 10 && profileData.completeness < 0.7f
            }
        }
    }
}

/**
 * Tracks which profile fields the user has completed
 */
data class ProfileCompleteness(
    val hasFitnessLevel: Boolean = false,
    val hasGoals: Boolean = false,
    val hasBodyStats: Boolean = false,        // Height and/or weight
    val hasPreferredExercises: Boolean = false,
    val hasReminderTime: Boolean = false,
    val hasProfilePhoto: Boolean = false,
    val hasDateOfBirth: Boolean = false,
    val hasGender: Boolean = false
) {
    /**
     * Calculate overall profile completeness (0.0 to 1.0)
     */
    val completeness: Float
        get() {
            val fields = listOf(
                hasFitnessLevel,
                hasGoals,
                hasBodyStats,
                hasPreferredExercises,
                hasProfilePhoto,
                hasDateOfBirth,
                hasGender
                // Don't include reminder time - it's optional
            )
            val completed = fields.count { it }
            return completed.toFloat() / fields.size
        }

    /**
     * Get friendly percentage string
     */
    val percentageString: String
        get() = "${(completeness * 100).toInt()}%"

    /**
     * Is profile considered "complete"? (>= 70%)
     */
    val isComplete: Boolean
        get() = completeness >= 0.7f

    companion object {
        /**
         * Create from UserDto
         */
        fun from(user: UserDto): ProfileCompleteness {
            return ProfileCompleteness(
                hasFitnessLevel = user.fitnessLevel != null,
                hasGoals = !user.goalsJson.isNullOrBlank(),
                hasBodyStats = user.height != null || user.weight != null,
                hasPreferredExercises = !user.preferredExercises.isNullOrBlank(),
                hasReminderTime = !user.reminderTime.isNullOrBlank(),
                hasProfilePhoto = !user.photoUrl.isNullOrBlank() || !user.photoCloudUrl.isNullOrBlank(),
                hasDateOfBirth = user.dateOfBirth != null,
                hasGender = user.gender != null
            )
        }
    }
}
