package org.liftrr.domain.analytics

import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseAnalyzer
import kotlin.math.pow
import kotlin.math.sqrt

class BenchPressAnalyzer : ExerciseSpecificAnalyzer {
    override fun analyze(frames: List<PoseDetectionResult.Success>): ExerciseSpecificMetrics {
        val flares = frames.mapNotNull { calculateElbowFlare(it) }
        val elbows = frames.mapNotNull { getBilateralAngle(it, 11, 13, 15) }

        return ExerciseSpecificMetrics.BenchPressMetrics(
            barPath = null,
            elbowAngle = ElbowAngleAnalysis(
                elbows.minOrNull() ?: 0f,
                elbows.maxOrNull() ?: 180f,
                calculateConsistency(elbows),
                100f - flares.average().toFloat()
            ),
            shoulderPosition = ShoulderAnalysis(85f, 80f, 90f, emptyList()),
            touchPointConsistency = 88f,
            archMaintenance = 85f
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

    private fun calculateElbowFlare(pose: PoseDetectionResult.Success): Float? {
        val lH = pose.getLandmark(23)
        val lS = pose.getLandmark(11)
        val lE = pose.getLandmark(13)
        val rH = pose.getLandmark(24)
        val rS = pose.getLandmark(12)
        val rE = pose.getLandmark(14)

        val left = if (lH != null && lS != null && lE != null) PoseAnalyzer.calculateAngle(
            lH, lS, lE
        ) else null
        val right = if (rH != null && rS != null && rE != null) PoseAnalyzer.calculateAngle(
            rH, rS, rE
        ) else null

        return when {
            left != null && right != null -> (left + right) / 2f
            else -> left ?: right
        }
    }

    private fun calculateConsistency(values: List<Float>): Float {
        if (values.size < 2) return 100f
        val avg = values.average().toFloat()
        val stdDev = sqrt(values.map { (it - avg).toDouble().pow(2.0) }.average()).toFloat()
        return (100f - (stdDev / avg.coerceAtLeast(1f) * 100f)).coerceIn(0f, 100f)
    }
}
