package org.liftrr.data.remote.dto.workout

data class RepDataDto(
    val repNumber: Int,
    val quality: Float,
    val isGoodForm: Boolean,
    val durationMs: Long,
    val timestamp: Long,
    val depth: Float? = null,
    val formIssues: List<String> = emptyList()
)
