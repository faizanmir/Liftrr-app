package org.liftrr.domain.analytics

class RecommendationGenerator {
    fun generate(
        session: WorkoutSession,
        reps: List<RepAnalysis>,
        rom: RangeOfMotionAnalysis,
        tempo: TempoAnalysis,
        symmetry: SymmetryAnalysis,
        metrics: ExerciseSpecificMetrics?
    ): List<String> {
        val recs = mutableListOf<String>()

        // 1. SAFETY CRITICALS (High Priority)
        when (metrics) {
            is ExerciseSpecificMetrics.DeadliftMetrics -> {
                if (metrics.backStraightness.spineNeutral < AnalysisConstants.CRITICAL_SPINE_ROUNDING_THRESHOLD) {
                    recs.add("CRITICAL: Spine rounding detected. Lower the weight and focus on a neutral lower back.")
                }
                if (metrics.lockoutCompletion < AnalysisConstants.LOCKOUT_COMPLETION_THRESHOLD) {
                    recs.add("Drive your hips through fully at the top to complete the lockout.")
                }
            }

            is ExerciseSpecificMetrics.SquatMetrics -> {
                if (metrics.kneeTracking.kneeCavePercentage > AnalysisConstants.KNEE_CAVE_PERCENTAGE_THRESHOLD) {
                    recs.add("Knees caving: Think about 'screwing' your feet into the floor to drive your knees out.")
                }
                if (metrics.torsoAngle.averageForwardLean > AnalysisConstants.TORSO_LEAN_THRESHOLD) {
                    recs.add("You're leaning too far forward. Keep your chest up and lead with your heart.")
                }
            }

            is ExerciseSpecificMetrics.BenchPressMetrics -> {
                if (metrics.elbowAngle.tucking < AnalysisConstants.SHOULDER_SAFETY_TUCKING_THRESHOLD) {
                    recs.add("Shoulder Safety: Tuck your elbows slightly more toward your ribs to avoid shoulder strain.")
                }
            }

            null -> {}
        }

        // 2. CONSISTENCY & ROM (Technical Proficiency)
        if (rom.averageDepth > AnalysisConstants.SHALLOW_SQUAT_THRESHOLD && session.exerciseType == org.liftrr.ml.ExerciseType.SQUAT) {
            recs.add("Work on your mobility; your squats are currently a bit shallow.")
        } else if (rom.consistency < AnalysisConstants.ROM_CONSISTENCY_THRESHOLD) {
            recs.add("Consistency is key. Focus on hitting the same depth/turnaround point on every rep.")
        }

        // 3. TEMPO & CONTROL (Rhythm)
        val avgEccentric = reps.mapNotNull { it.tempo?.eccentricMs }.average()
        val avgConcentric = reps.mapNotNull { it.tempo?.concentricMs }.average()

        if (avgEccentric < AnalysisConstants.SLOW_ECCENTRIC_THRESHOLD) {
            recs.add("Slow down your descent. Aim for a 2-second 'eccentric' phase for better muscle growth.")
        }

        // Check for "grinding" reps (slow concentric)
        if (avgConcentric > AnalysisConstants.GRINDING_REP_THRESHOLD && session.goodReps > 0) {
            recs.add("Your rep speed is slowing down significantly. You're approaching technical failure.")
        }

        // 4. SYMMETRY (Balance)
        if (symmetry.overallSymmetry < AnalysisConstants.SYMMETRY_IMBALANCE_THRESHOLD) {
            recs.add("You're favoring one side. Focus on an even weight distribution across both feet.")
        }

        // 5. REINFORCEMENT
        if (recs.isEmpty() && session.goodReps.toFloat() / session.totalReps > AnalysisConstants.ELITE_PERFORMANCE_THRESHOLD) {
            recs.add("Masterful performance! Your technique and consistency are elite level.")
        }

        return recs.ifEmpty { listOf("Solid session. Keep maintaining this level of control.") }
    }
}
