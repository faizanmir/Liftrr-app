package org.liftrr.domain.workout

/**
 * Represents a key moment in the workout that should be saved as a visual reference
 */
data class KeyFrame(
    val timestamp: Long,
    val repNumber: Int,
    val frameType: KeyFrameType,
    val imagePath: String,  // Path to saved image with skeletal overlay
    val description: String,
    val formScore: Float? = null
)

/**
 * Types of key frames to capture during workout
 */
enum class KeyFrameType {
    BEST_REP,           // Rep with highest form score
    WORST_REP,          // Rep with lowest form score
    FORM_ISSUE,         // Moment showing specific form issue
    PROGRESSION,        // Shows progression throughout workout (early, mid, late)
    REFERENCE,          // Additional reference frames for quality analysis
    BOTTOM_POSITION,    // Deepest/lowest point of movement
    TOP_POSITION,       // Top/lockout position
    GOOD_EXAMPLE,       // Example of good form for comparison
    BAD_EXAMPLE         // Example of what not to do
}

/**
 * Metadata for captured frames during workout
 */
data class CapturedFrame(
    val bitmap: android.graphics.Bitmap,
    val timestamp: Long,
    val repNumber: Int,
    val poseData: org.liftrr.ml.PoseDetectionResult.Success,
    val formScore: Float,
    val formIssues: List<String>
)
