package org.liftrr.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.liftrr.data.models.PromptType
import org.liftrr.data.models.UserPromptEntity

@Dao
interface UserPromptDao {

    /**
     * Record that a prompt was shown to the user
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prompt: UserPromptEntity): Long

    /**
     * Get prompt history for a specific user and prompt type
     */
    @Query("SELECT * FROM user_prompts WHERE userId = :userId AND promptType = :promptType")
    suspend fun getPrompt(userId: String, promptType: PromptType): UserPromptEntity?

    /**
     * Get all prompt history for a user
     */
    @Query("SELECT * FROM user_prompts WHERE userId = :userId ORDER BY lastShownAt DESC")
    suspend fun getAllPrompts(userId: String): List<UserPromptEntity>

    /**
     * Observe prompt history for a user
     */
    @Query("SELECT * FROM user_prompts WHERE userId = :userId ORDER BY lastShownAt DESC")
    fun observePrompts(userId: String): Flow<List<UserPromptEntity>>

    /**
     * Mark a prompt as completed
     */
    @Query("""
        UPDATE user_prompts
        SET completed = 1,
            completedAt = :timestamp,
            shouldShowAgain = 0
        WHERE userId = :userId AND promptType = :promptType
    """)
    suspend fun markCompleted(
        userId: String,
        promptType: PromptType,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Mark a prompt as dismissed
     */
    @Query("""
        UPDATE user_prompts
        SET dismissed = 1,
            dismissedAt = :timestamp
        WHERE userId = :userId AND promptType = :promptType
    """)
    suspend fun markDismissed(
        userId: String,
        promptType: PromptType,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Mark a prompt as permanently dismissed (won't show again)
     */
    @Query("""
        UPDATE user_prompts
        SET dismissed = 1,
            dismissedAt = :timestamp,
            shouldShowAgain = 0
        WHERE userId = :userId AND promptType = :promptType
    """)
    suspend fun markPermanentlyDismissed(
        userId: String,
        promptType: PromptType,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Record that a prompt was shown (increment counter, update timestamp)
     */
    @Query("""
        UPDATE user_prompts
        SET lastShownAt = :timestamp,
            timesShown = timesShown + 1,
            dismissed = 0
        WHERE userId = :userId AND promptType = :promptType
    """)
    suspend fun recordShown(
        userId: String,
        promptType: PromptType,
        timestamp: Long = System.currentTimeMillis()
    ): Int

    /**
     * Insert or update prompt shown tracking
     */
    @Transaction
    suspend fun trackPromptShown(userId: String, promptType: PromptType) {
        val existing = getPrompt(userId, promptType)
        val now = System.currentTimeMillis()

        if (existing == null) {
            // First time showing this prompt
            insert(
                UserPromptEntity(
                    userId = userId,
                    promptType = promptType,
                    firstShownAt = now,
                    lastShownAt = now,
                    timesShown = 1
                )
            )
        } else {
            // Update existing record
            recordShown(userId, promptType, now)
        }
    }

    /**
     * Get count of completed prompts for a user
     */
    @Query("SELECT COUNT(*) FROM user_prompts WHERE userId = :userId AND completed = 1")
    suspend fun getCompletedCount(userId: String): Int

    /**
     * Check if a specific prompt has been completed
     */
    @Query("SELECT completed FROM user_prompts WHERE userId = :userId AND promptType = :promptType")
    suspend fun isCompleted(userId: String, promptType: PromptType): Boolean?

    /**
     * Delete all prompts for a user (useful for testing or account deletion)
     */
    @Query("DELETE FROM user_prompts WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    /**
     * Delete specific prompt
     */
    @Delete
    suspend fun delete(prompt: UserPromptEntity)
}
