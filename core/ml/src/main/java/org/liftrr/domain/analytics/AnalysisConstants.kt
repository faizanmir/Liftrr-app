package org.liftrr.domain.analytics

object AnalysisConstants {
    // Form Issues
    const val KNEE_CAVE_THRESHOLD = 0.88f
    const val TORSO_COLLAPSE_THRESHOLD = 45f
    const val ROUNDED_BACK_THRESHOLD = 0.82f
    const val EXCESSIVE_ELBOW_FLARE_THRESHOLD = 80f

    // Symmetry
    const val IMBALANCED_DRIVE_THRESHOLD = 85f

    // Form Consistency
    const val FORM_TREND_IMPROVEMENT_THRESHOLD = 0.1
    const val FORM_TREND_DECLINE_THRESHOLD = -0.1

    // Recommendations
    const val CRITICAL_SPINE_ROUNDING_THRESHOLD = 80f
    const val LOCKOUT_COMPLETION_THRESHOLD = 90f
    const val KNEE_CAVE_PERCENTAGE_THRESHOLD = 25f
    const val TORSO_LEAN_THRESHOLD = 45f
    const val SHOULDER_SAFETY_TUCKING_THRESHOLD = 60f
    const val SHALLOW_SQUAT_THRESHOLD = 115f
    const val ROM_CONSISTENCY_THRESHOLD = 75f
    const val SLOW_ECCENTRIC_THRESHOLD = 800
    const val GRINDING_REP_THRESHOLD = 2500
    const val SYMMETRY_IMBALANCE_THRESHOLD = 82f
    const val ELITE_PERFORMANCE_THRESHOLD = 0.9f

    // Squat Metrics
    const val DEEP_SQUAT_ANGLE = 95f
    const val PARALLEL_SQUAT_ANGLE = 110f
}
