package org.liftrr.domain.analytics

import org.liftrr.domain.workout.RepData
import org.liftrr.ml.ExerciseType
import org.liftrr.ml.PoseAnalyzer
import org.liftrr.ml.PoseDetectionResult
import kotlin.math.abs
import kotlin.math.atan2

class RepAnalyzer {

    fun analyze(rep: RepData, frames: List<PoseFrame>, type: ExerciseType): RepAnalysis {
        val successFrames = frames.mapNotNull { it.poseResult as? PoseDetectionResult.Success }

        if (successFrames.isEmpty()) {
            return RepAnalysis(
                repNumber = rep.repNumber,
                isGoodForm = rep.isGoodForm,
                quality = rep.poseQuality,
                depth = null,
                tempo = null,
                peakFrameIndex = null,
                formIssues = listOf("Insufficient pose data")
            )
        }

        val peakIdx = findActualPeakFrame(successFrames, type)
        return RepAnalysis(
            repNumber = rep.repNumber,
            isGoodForm = rep.isGoodForm,
            quality = rep.poseQuality,
            depth = calculateRepDepth(successFrames, type),
            tempo = calculateAccurateTempo(frames, peakIdx),
            peakFrameIndex = peakIdx,
            formIssues = identifyFormIssues(successFrames, type)
        )
    }

    private fun findActualPeakFrame(
        frames: List<PoseDetectionResult.Success>, type: ExerciseType
    ): Int {
        val angles = frames.map { pose ->
            getBilateralAngle(pose, getPrimaryJoints(type)) ?: 180f
        }
        // Deadlift peak is max extension (top); others are min angle (bottom)
        return if (type == ExerciseType.DEADLIFT) angles.indexOf(angles.maxOrNull()) else angles.indexOf(
            angles.minOrNull()
        )
    }

    private fun calculateAccurateTempo(frames: List<PoseFrame>, peakIndex: Int): RepTempo? {
        if (frames.size < 2) return null
        val start = frames.first().timestamp
        val peak = frames.getOrNull(peakIndex)?.timestamp ?: start
        val end = frames.last().timestamp
        return RepTempo(
            totalMs = end - start, eccentricMs = peak - start, concentricMs = end - peak
        )
    }

    private fun calculateRepDepth(
        frames: List<PoseDetectionResult.Success>, type: ExerciseType
    ): Float? {
        val angles = frames.mapNotNull { getBilateralAngle(it, getPrimaryJoints(type)) }
        return if (type == ExerciseType.DEADLIFT) angles.maxOrNull() else angles.minOrNull()
    }

    private fun identifyFormIssues(
        frames: List<PoseDetectionResult.Success>, type: ExerciseType
    ): List<String> {
        val issues = mutableSetOf<String>()
        frames.forEach { pose ->
            when (type) {
                ExerciseType.SQUAT -> {
                    if (calculateValgusRatio(pose) < AnalysisConstants.KNEE_CAVE_THRESHOLD) issues.add("Knee Cave")
                    calculateForwardLean(pose)?.let { if (it > AnalysisConstants.TORSO_COLLAPSE_THRESHOLD) issues.add("Torso Collapse") }
                }

                ExerciseType.DEADLIFT -> {
                    if (calculateBackAlignment(pose) < AnalysisConstants.ROUNDED_BACK_THRESHOLD) issues.add("Rounded Back")
                }

                ExerciseType.BENCH_PRESS -> {
                    calculateElbowFlare(pose)?.let { if (it > AnalysisConstants.EXCESSIVE_ELBOW_FLARE_THRESHOLD) issues.add("Excessive Flare") }
                }
            }
        }
        return issues.toList()
    }

    private fun getBilateralAngle(
        pose: PoseDetectionResult.Success, joints: Triple<Int, Int, Int>
    ) = getBilateralAngle(pose, joints.first, joints.second, joints.third)

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

    private fun calculateValgusRatio(pose: PoseDetectionResult.Success): Float {
        val lK = pose.getLandmark(25)
        val rK = pose.getLandmark(26)
        val lA = pose.getLandmark(27)
        val rA = pose.getLandmark(28)
        if (lK == null || rK == null || lA == null || rA == null) return 1.0f
        return abs(lK.x() - rK.x()) / abs(lA.x() - rA.x()).coerceAtLeast(0.01f)
    }

    private fun calculateForwardLean(pose: PoseDetectionResult.Success): Float? {
        val s = pose.getLandmark(11) ?: return null
        val h = pose.getLandmark(23) ?: return null
        return Math.toDegrees(atan2(abs(s.x() - h.x()).toDouble(), abs(s.y() - h.y()).toDouble()))
            .toFloat()
    }

    private fun calculateBackAlignment(pose: PoseDetectionResult.Success): Float {
        val s = pose.getLandmark(11)
        val h = pose.getLandmark(23)
        val k = pose.getLandmark(25)
        if (s == null || h == null || k == null) return 1.0f
        return abs(s.y() - h.y()) / abs(h.y() - k.y()).coerceAtLeast(0.01f)
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

    private fun getPrimaryJoints(type: ExerciseType) = when (type) {
        ExerciseType.SQUAT -> Triple(23, 25, 27)
        ExerciseType.DEADLIFT -> Triple(11, 23, 25)
        ExerciseType.BENCH_PRESS -> Triple(11, 13, 15)
    }
}
