package org.liftrr.domain.analytics

import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseAnalyzer
import kotlin.math.abs

class DeadliftAnalyzer : ExerciseSpecificAnalyzer {
    override fun analyze(frames: List<PoseDetectionResult.Success>): ExerciseSpecificMetrics {
        val backs = frames.map { calculateBackAlignment(it) }
        val lockouts = frames.mapNotNull { getBilateralAngle(it, 11, 23, 25) }
        val worstBack = backs.minOrNull() ?: 1.0f

        return ExerciseSpecificMetrics.DeadliftMetrics(
            hipHingeQuality = 85f,
            backStraightness = BackAnalysis(
                worstBack * 100f, if (worstBack < AnalysisConstants.ROUNDED_BACK_THRESHOLD) 20f else 0f, 0f, emptyList()
            ),
            lockoutCompletion = ((lockouts.maxOrNull() ?: 0f) / 170f * 100f).coerceIn(0f, 100f),
            barPath = null,
            startingPosition = StartPositionAnalysis(85f, 85f, 85f, emptyList())
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

    private fun calculateBackAlignment(pose: PoseDetectionResult.Success): Float {
        val s = pose.getLandmark(11)
        val h = pose.getLandmark(23)
        val k = pose.getLandmark(25)
        if (s == null || h == null || k == null) return 1.0f
        return abs(s.y() - h.y()) / abs(h.y() - k.y()).coerceAtLeast(0.01f)
    }
}
