package org.liftrr.data.local.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.liftrr.data.models.dto.UserProfileEntity
import org.liftrr.domain.user.FitnessLevel
import org.liftrr.domain.user.Gender

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfileByUserId(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun observeProfile(userId: String): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Query("""
        UPDATE user_profiles SET
            firstName   = COALESCE(:firstName, firstName),
            lastName    = COALESCE(:lastName, lastName),
            gender      = COALESCE(:gender, gender),
            height      = COALESCE(:height, height),
            fitnessLevel = COALESCE(:fitnessLevel, fitnessLevel),
            goalsJson   = COALESCE(:goalsJson, goalsJson),
            updatedAt   = :updatedAt
        WHERE userId = :userId
    """)
    suspend fun updateFields(
        userId: String,
        firstName: String?,
        lastName: String?,
        gender: Gender?,
        height: Float?,
        fitnessLevel: FitnessLevel?,
        goalsJson: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}
