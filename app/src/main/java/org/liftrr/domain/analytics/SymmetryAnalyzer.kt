package org.liftrr.domain.analytics

import org.liftrr.domain.analytics.AnalysisConstants
import org.liftrr.ml.ExerciseType
import org.liftrr.ml.PoseAnalyzer
import org.liftrr.ml.PoseDetectionResult
import kotlin.math.abs

class SymmetryAnalyzer {

    fun analyze(session: WorkoutSession): SymmetryAnalysis {
        val frames = session.poseFrames.mapNotNull { it.poseResult as? PoseDetectionResult.Success }
        val scores = frames.mapNotNull { calculateSymmetryScore(it, session.exerciseType) }
        val avg = if (scores.isNotEmpty()) scores.average().toFloat() else 100f
        return SymmetryAnalysis(
            avg, 100f - avg, if (avg < AnalysisConstants.IMBALANCED_DRIVE_THRESHOLD) listOf("Imbalanced drive") else emptyList()
        )
    }

    private fun calculateSymmetryScore(
        pose: PoseDetectionResult.Success, type: ExerciseType
    ): Float? {
        val joints = getPrimaryJoints(type)
        val l = getBilateralAngle(
            pose, joints.first, joints.second, joints.third
        ) // Simplified using our own helper
        val r = getBilateralAngle(pose, joints.first + 1, joints.second + 1, joints.third + 1)
        if (l == null || r == null) return null
        return (100f - (abs(l - r) / l.coerceAtLeast(r).coerceAtLeast(1f) * 100f)).coerceIn(
            0f, 100f
        )
    }

    private fun getBilateralAngle(
        pose: PoseDetectionResult.Success, a: Int, b: Int, c: Int
    ): Float? {
        val lA = pose.getLandmark(a)
        val lB = pose.getLandmark(b)
        val lC = pose.getLandmark(c)
        val rA = pose.getLandmark(a + 1)
        val rB = pose.getLandmark(b + 1)
        val rC = pose.getLandmark(c + 1)

        val left = if (lA != null && lB != null && lC != null) PoseAnalyzer.calculateAngle(
            lA, lB, lC
        ) else null
        val right = if (rA != null && rB != null && rC != null) PoseAnalyzer.calculateAngle(
            rA, rB, rC
        ) else null

        return when {
            left != null && right != null -> (left + right) / 2f
            else -> left ?: right
        }
    }

    private fun getPrimaryJoints(type: ExerciseType) = when (type) {
        ExerciseType.SQUAT -> Triple(23, 25, 27)
        ExerciseType.DEADLIFT -> Triple(11, 23, 25)
        ExerciseType.BENCH_PRESS -> Triple(11, 13, 15)
    }
}
