package org.liftrr.data.repository.mappers

import org.liftrr.data.models.dto.UserWeightDto
import org.liftrr.domain.weight.UserWeight

fun UserWeightDto.toDomain(): UserWeight {
    return UserWeight(
        exerciseType = type,
        weight = weight
    )
}

fun UserWeight.toDto() =
    UserWeightDto(
        type = exerciseType,
        weight = weight
    )