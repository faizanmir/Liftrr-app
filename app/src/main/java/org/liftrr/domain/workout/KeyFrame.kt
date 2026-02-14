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
    val formScore: Float? = null,
    val movementPhase: MovementPhase? = null,  // Phase of movement (setup, bottom, lockout, etc.)
    val diagnostics: List<FormDiagnostic> = emptyList()  // Detailed form diagnostics with angles
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
 * Represents different phases of a lift movement
 */
enum class MovementPhase {
    SETUP,          // Starting position before movement begins
    DESCENT,        // Descending/lowering phase (squat down, bar down, etc.)
    BOTTOM,         // Deepest/lowest point of the movement
    ASCENT,         // Ascending/rising phase (squat up, bar up, etc.)
    LOCKOUT,        // Completion/top position with full extension
    TRANSITION      // Transitional positions between phases
}

/**
 * Detailed diagnostic information about form issues with specific angle measurements
 */
data class FormDiagnostic(
    val issue: String,              // e.g., "Shallow depth", "Knees caving in"
    val angle: String,              // e.g., "Hip angle", "Knee angle"
    val measured: Float,            // Actual measured angle in degrees
    val expected: String,           // Expected range e.g., "< 90°", "160-180°"
    val severity: FormIssueSeverity // How critical this issue is
)

enum class FormIssueSeverity {
    MINOR,      // Small deviation, not critical
    MODERATE,   // Noticeable issue that should be addressed
    CRITICAL    // Major form break that could lead to injury
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
    val formIssues: List<String>,
    val movementPhase: MovementPhase = MovementPhase.LOCKOUT,  // Default to lockout for backward compatibility
    val diagnostics: List<FormDiagnostic> = emptyList()  // Detailed form diagnostics with angles
)
