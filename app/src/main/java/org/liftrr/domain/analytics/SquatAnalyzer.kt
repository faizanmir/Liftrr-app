package org.liftrr.domain.analytics

import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseAnalyzer
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class SquatAnalyzer : ExerciseSpecificAnalyzer {
    override fun analyze(frames: List<PoseDetectionResult.Success>): ExerciseSpecificMetrics.SquatMetrics {
        val depths = frames.mapNotNull { getBilateralAngle(it, 23, 25, 27) }
        val leans = frames.mapNotNull { calculateForwardLean(it) }
        val valgus = frames.map { calculateValgusRatio(it) }

        val minDepth = depths.minOrNull() ?: 180f
        val cavePct = (valgus.count { it < AnalysisConstants.KNEE_CAVE_THRESHOLD }.toFloat() / valgus.size.coerceAtLeast(1)) * 100f

        return ExerciseSpecificMetrics.SquatMetrics(
            averageDepth = depths.average().toFloat(),
            depthConsistency = calculateConsistency(depths),
            kneeTracking = KneeTrackingAnalysis(
                valgus.average().toFloat() * 100f, cavePct, 85f, emptyList()
            ),
            hipMobility = calculateHipMobilityScore(minDepth),
            torsoAngle = TorsoAngleAnalysis(
                leans.average().toFloat(), calculateConsistency(leans), leans.average() > AnalysisConstants.TORSO_LEAN_THRESHOLD
            ),
            balance = BalanceAnalysis(85f, 80f, 90f, emptyList())
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

    private fun calculateForwardLean(pose: PoseDetectionResult.Success): Float? {
        val s = pose.getLandmark(11) ?: return null
        val h = pose.getLandmark(23) ?: return null
        return Math.toDegrees(atan2(abs(s.x() - h.x()).toDouble(), abs(s.y() - h.y()).toDouble()))
            .toFloat()
    }

    private fun calculateConsistency(values: List<Float>): Float {
        if (values.size < 2) return 100f
        val avg = values.average().toFloat()
        val stdDev = sqrt(values.map { (it - avg).toDouble().pow(2.0) }.average()).toFloat()
        return (100f - (stdDev / avg.coerceAtLeast(1f) * 100f)).coerceIn(0f, 100f)
    }

    private fun calculateHipMobilityScore(minAngle: Float): Float = when {
        minAngle <= AnalysisConstants.DEEP_SQUAT_ANGLE -> 100f
        minAngle <= AnalysisConstants.PARALLEL_SQUAT_ANGLE -> 85f
        else -> 60f
    }

    private fun calculateValgusRatio(pose: PoseDetectionResult.Success): Float {
        val lK = pose.getLandmark(25)
        val rK = pose.getLandmark(26)
        val lA = pose.getLandmark(27)
        val rA = pose.getLandmark(28)
        if (lK == null || rK == null || lA == null || rA == null) return 1.0f
        return abs(lK.x() - rK.x()) / abs(lA.x() - rA.x()).coerceAtLeast(0.01f)
    }
}
