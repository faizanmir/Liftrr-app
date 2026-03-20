package org.liftrr.data.remote.dto.auth

import org.liftrr.data.models.dto.UserDto

sealed class AuthResult {
    data class Success(val user: UserDto) : AuthResult()
    data class Error(val message: String) : AuthResult()
}