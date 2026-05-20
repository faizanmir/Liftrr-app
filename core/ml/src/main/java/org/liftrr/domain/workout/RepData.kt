package org.liftrr.domain.workout

data class RepData(
    val repNumber: Int,
    val timestamp: Long,
    val poseQuality: Float,
    val isGoodForm: Boolean,
    val formScore: Float = 100f,
    val feedback: List<FormFeedback> = emptyList()
)
