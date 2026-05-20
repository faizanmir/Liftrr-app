package org.liftrr.data.remote.dto.weight

data class WeightEntryResponse(
    val id: String,
    val exerciseType: String,
    val weight: Float,
    val timestamp: Long,
    val createdAt: Long
)

data class BulkWeightUpsertRequest(
    val entries: List<WeightEntryRequest>
)

data class WeightEntryRequest(
    val exerciseType: String,
    val weight: Float,
    val timestamp: Long
)
