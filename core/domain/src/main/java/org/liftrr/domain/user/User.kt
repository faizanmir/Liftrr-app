package org.liftrr.domain.user

data class User(
    val userId: String,
    val firstName: String?,
    val lastName: String?,
    val email: String,
    val authProvider: AuthProvider,
    val photoUrl: String? = null,
    val photoCloudUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
