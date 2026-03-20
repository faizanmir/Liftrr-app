package org.liftrr.data.repository.mappers

import org.liftrr.data.remote.dto.auth.AuthResult as DataAuthResult
import org.liftrr.domain.auth.AuthResult

fun DataAuthResult.toDomain(): AuthResult = when (this) {
    is DataAuthResult.Success -> AuthResult.Success(user.toDomain())
    is DataAuthResult.Error -> AuthResult.Error(message)
}
