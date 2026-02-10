package org.liftrr.domain.analytics

import org.liftrr.ml.ExerciseType

/**
 * Comprehensive workout report with all analysis results
 *
 * Generated after a workout session completes.
 * Contains high-level metrics and detailed per-rep analysis.
 */
data class WorkoutReport(
    val sessionId: String,
    val exerciseType: ExerciseType,
    val totalReps: Int,
    val goodReps: Int,
    val badReps: Int,
    val averageQuality: Float,
    val durationMs: Long,
    val rangeOfMotion: RangeOfMotionAnalysis,
    val tempo: TempoAnalysis,
    val symmetry: SymmetryAnalysis,
    val formConsistency: FormConsistencyAnalysis,
    val repAnalyses: List<RepAnalysis>,
    val recommendations: List<String>,
    val exerciseSpecificMetrics: ExerciseSpecificMetrics? = null
) {
    /**
     * Get overall workout score (0-100)
     */
    val overallScore: Float
        get() {
            val formScore = (goodReps.toFloat() / totalReps.coerceAtLeast(1)) * 100f
            val qualityScore = averageQuality * 100f
            val romScore = rangeOfMotion.consistency
            val symmetryScore = symmetry.overallSymmetry
            val consistencyScore = formConsistency.consistencyScore

            return (formScore + qualityScore + romScore + symmetryScore + consistencyScore) / 5f
        }

    /**
     * Get grade (A, B, C, D, F)
     */
    val grade: String
        get() = when {
            overallScore >= 90f -> "A"
            overallScore >= 80f -> "B"
            overallScore >= 70f -> "C"
            overallScore >= 60f -> "D"
            else -> "F"
        }
}

/**
 * Analysis of a single rep
 */
data class RepAnalysis(
    val repNumber: Int,
    val isGoodForm: Boolean,
    val quality: Float,
    val depth: Float?, // Depth/ROM for this rep
    val tempo: RepTempo?, // Timing for this rep
    val peakFrameIndex: Int?, // Frame number at deepest point
    val formIssues: List<String>
)

/**
 * Timing breakdown for a single rep
 */
data class RepTempo(
    val totalMs: Long,
    val eccentricMs: Long, // Lowering phase duration
    val concentricMs: Long  // Lifting phase duration
)

/**
 * Range of motion analysis across all reps
 */
data class RangeOfMotionAnalysis(
    val averageDepth: Float,
    val minDepth: Float,
    val maxDepth: Float,
    val consistency: Float // 0-100, higher = more consistent
)

/**
 * Tempo/timing analysis across all reps
 */
data class TempoAnalysis(
    val averageRepDurationMs: Long,
    val averageRestBetweenRepsMs: Long,
    val tempoConsistency: Float // 0-100, higher = more consistent
)

/**
 * Symmetry analysis (left vs right)
 */
data class SymmetryAnalysis(
    val overallSymmetry: Float, // 0-100, higher = more symmetrical
    val leftRightAngleDifference: Float,
    val issues: List<String>
)

/**
 * Form consistency analysis
 */
data class FormConsistencyAnalysis(
    val consistencyScore: Float, // 0-100
    val qualityTrend: String // "Improving", "Declining", "Stable"
)

/**
 * Video highlight metadata
 * Points to specific moments in the workout video
 */
data class VideoHighlight(
    val type: HighlightType,
    val timestampMs: Long,
    val frameNumber: Int,
    val description: String
)

enum class HighlightType {
    BEST_REP,
    WORST_REP,
    FORM_ISSUE,
    PERSONAL_BEST,
    KEY_MOMENT
}

/**
 * Exercise-specific metrics that vary by exercise type
 */
sealed class ExerciseSpecificMetrics {

    /**
     * Squat-specific metrics
     */
    data class SquatMetrics(
        val averageDepth: Float,              // Hip angle at bottom (degrees)
        val depthConsistency: Float,          // 0-100
        val kneeTracking: KneeTrackingAnalysis,
        val hipMobility: Float,               // 0-100, ability to reach proper depth
        val torsoAngle: TorsoAngleAnalysis,
        val balance: BalanceAnalysis
    ) : ExerciseSpecificMetrics()

    /**
     * Deadlift-specific metrics
     */
    data class DeadliftMetrics(
        val hipHingeQuality: Float,           // 0-100, proper hip hinge pattern
        val backStraightness: BackAnalysis,
        val lockoutCompletion: Float,         // 0-100, full hip extension at top
        val barPath: BarPathAnalysis?,        // Would need tracking
        val startingPosition: StartPositionAnalysis
    ) : ExerciseSpecificMetrics()

    /**
     * Bench Press-specific metrics
     */
    data class BenchPressMetrics(
        val barPath: BarPathAnalysis?,        // Would need tracking
        val elbowAngle: ElbowAngleAnalysis,
        val shoulderPosition: ShoulderAnalysis,
        val touchPointConsistency: Float,     // 0-100
        val archMaintenance: Float            // 0-100
    ) : ExerciseSpecificMetrics()
}

/**
 * Knee tracking analysis for squats
 */
data class KneeTrackingAnalysis(
    val kneeAlignment: Float,           // 0-100, knees tracking over toes
    val kneeCavePercentage: Float,      // 0-100, % of reps with knee cave
    val lateralStability: Float,        // 0-100
    val issues: List<String>
)

/**
 * Torso angle analysis
 */
data class TorsoAngleAnalysis(
    val averageForwardLean: Float,      // Degrees from vertical
    val consistency: Float,             // 0-100
    val excessiveLean: Boolean
)

/**
 * Balance and stability analysis
 */
data class BalanceAnalysis(
    val weightDistribution: Float,      // 0-100, even distribution
    val heelPressure: Float,            // 0-100, staying on heels
    val stability: Float,               // 0-100, minimal swaying
    val issues: List<String>
)

/**
 * Back straightness analysis (deadlifts)
 */
data class BackAnalysis(
    val spineNeutral: Float,            // 0-100, maintaining neutral spine
    val lowerBackRounding: Float,       // 0-100, lower back rounding detected
    val upperBackRounding: Float,       // 0-100, upper back rounding detected
    val issues: List<String>
)

/**
 * Bar path analysis (deadlift, bench press)
 */
data class BarPathAnalysis(
    val verticality: Float,             // 0-100, how vertical the bar path is
    val consistency: Float,             // 0-100, path consistency across reps
    val deviations: List<String>
)

/**
 * Starting position analysis (deadlift)
 */
data class StartPositionAnalysis(
    val hipHeight: Float,               // 0-100, appropriate hip height
    val shoulderPosition: Float,        // 0-100, shoulders over bar
    val setup: Float,                   // 0-100, overall setup quality
    val issues: List<String>
)

/**
 * Elbow angle analysis (bench press)
 */
data class ElbowAngleAnalysis(
    val bottomAngle: Float,             // Degrees at bottom of press
    val topAngle: Float,                // Degrees at top (lockout)
    val consistency: Float,             // 0-100
    val tucking: Float                  // 0-100, proper elbow tuck
)

/**
 * Shoulder position analysis (bench press)
 */
data class ShoulderAnalysis(
    val retraction: Float,              // 0-100, shoulder blade retraction
    val depression: Float,              // 0-100, shoulders down
    val stability: Float,               // 0-100, shoulder stability
    val issues: List<String>
)
