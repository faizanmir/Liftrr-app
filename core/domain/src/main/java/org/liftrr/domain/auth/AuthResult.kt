package org.liftrr.domain.auth

import org.liftrr.domain.user.User

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
