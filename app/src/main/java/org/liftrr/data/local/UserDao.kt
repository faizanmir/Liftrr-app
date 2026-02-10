package org.liftrr.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.liftrr.data.models.UserDto

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAll(): Flow<List<UserDto>>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserDto?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserDto?

    @Query("SELECT * FROM users ORDER BY lastLoginAt DESC LIMIT 1")
    suspend fun getLastLoggedInUser(): UserDto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userDto: UserDto)

    @Delete
    suspend fun delete(userDto : UserDto)

    @Update
    suspend fun update(userDto : UserDto)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
