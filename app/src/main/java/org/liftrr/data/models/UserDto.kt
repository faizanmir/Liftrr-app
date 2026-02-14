package org.liftrr.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuthProvider {
    EMAIL_PASSWORD,
    GOOGLE,
    FACEBOOK,
    APPLE
}

@Entity(tableName = "users")
data class UserDto(
    @PrimaryKey val userId: String,
    val firstName: String?,
    val lastName: String?,
    val email: String,
    val passwordHash: String? = null,
    val authProvider: AuthProvider,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)