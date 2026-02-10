package org.liftrr.data.models

/**
 * Serializable rep data for database storage
 * Contains timing and quality metrics for each rep
 */
data class RepDataDto(
    val repNumber: Int,
    val quality: Float,
    val isGoodForm: Boolean,
    val durationMs: Long,
    val timestamp: Long
)
