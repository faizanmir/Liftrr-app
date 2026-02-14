package org.liftrr.domain.workout

import org.liftrr.ml.PoseAnalyzer
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseLandmarks
import kotlin.math.abs

class DeadliftExercise : Exercise {

    private var isAtBottom = false
    private var bottomFrameCount = 0
    private var lastRepTime = 0L

    private val hipAngleSmoother = AngleSmoother(3)
    private val trunkSmoother = AngleSmoother(3)

    private var lastSmoothedHipAngle = 180f
    private var lastSmoothedTrunkRatio = 1f

    private var wentTooLowDuringRep = false
    private var repMinTrunkAlignment = Float.MAX_VALUE
    private var repMinHipAngle = Float.MAX_VALUE
    private var repMaxLockoutAngle = 0f
    private var lastFormScore = 100f

    companion object {
        private const val MIN_FRAMES_FOR_STABILITY = 2
        private const val MIN_REP_DURATION_MS = 600L

        private const val BOTTOM_ENTRY_ANGLE = 115f
        private const val BOTTOM_EXIT_ANGLE = 130f
        private const val TOP_ANGLE_THRESHOLD = 160f
        private const val SQUAT_THRESHOLD = 80f

        private const val TRUNK_ALIGNMENT_THRESHOLD = 0.85f

        private const val SQUATTING_PENALTY = 40f
        private const val BACK_ROUNDING_PENALTY_WEIGHT = 30f
        private const val LOCKOUT_PENALTY_WEIGHT = 20f
    }

    override fun analyzeFeedback(pose: PoseDetectionResult.Success): String {
        val leftShoulder = pose.getLandmark(PoseLandmarks.LEFT_SHOULDER)
        val rightShoulder = pose.getLandmark(PoseLandmarks.RIGHT_SHOULDER)
        val leftHip = pose.getLandmark(PoseLandmarks.LEFT_HIP)
        val rightHip = pose.getLandmark(PoseLandmarks.RIGHT_HIP)
        val leftKnee = pose.getLandmark(PoseLandmarks.LEFT_KNEE)
        val rightKnee = pose.getLandmark(PoseLandmarks.RIGHT_KNEE)

        if (leftShoulder == null || rightShoulder == null || leftHip == null ||
            rightHip == null || leftKnee == null || rightKnee == null
        ) {
            return "Move into frame"
        }

        if (lastSmoothedTrunkRatio < TRUNK_ALIGNMENT_THRESHOLD && lastSmoothedHipAngle < BOTTOM_EXIT_ANGLE) {
            return "Keep back straight!"
        }

        return when {
            lastSmoothedHipAngle < SQUAT_THRESHOLD -> "Don't squat - hinge at hips!"
            lastSmoothedHipAngle < 100f -> "Too bent - engage hips"
            lastSmoothedHipAngle > 170f -> "Good lockout!"
            lastSmoothedHipAngle > TOP_ANGLE_THRESHOLD -> "Almost locked out"
            lastSmoothedHipAngle > 140f -> "Drive hips forward"
            else -> "Keep pulling"
        }
    }

    override fun updateRepCount(pose: PoseDetectionResult.Success): Boolean {
        val leftShoulder = pose.getLandmark(PoseLandmarks.LEFT_SHOULDER)
        val leftHip = pose.getLandmark(PoseLandmarks.LEFT_HIP)
        val leftKnee = pose.getLandmark(PoseLandmarks.LEFT_KNEE)

        if (leftShoulder == null || leftHip == null || leftKnee == null) {
            bottomFrameCount = 0
            return false
        }

        val hipAngle = PoseAnalyzer.calculateAngle(leftShoulder, leftHip, leftKnee)
        val smoothedHip = hipAngleSmoother.add(hipAngle)
        lastSmoothedHipAngle = smoothedHip

        if (smoothedHip < repMinHipAngle) repMinHipAngle = smoothedHip
        if (smoothedHip > repMaxLockoutAngle) repMaxLockoutAngle = smoothedHip

        val trunkRatio = calculateTrunkAlignment(leftShoulder, leftHip, leftKnee)
        val smoothedTrunk = trunkSmoother.add(trunkRatio)
        lastSmoothedTrunkRatio = smoothedTrunk
        if (smoothedTrunk < repMinTrunkAlignment) repMinTrunkAlignment = smoothedTrunk

        if (smoothedHip < SQUAT_THRESHOLD) {
            wentTooLowDuringRep = true
        }

        val isAtBottomPosition = smoothedHip < BOTTOM_ENTRY_ANGLE

        if (isAtBottomPosition) {
            bottomFrameCount++
            if (bottomFrameCount >= MIN_FRAMES_FOR_STABILITY && !isAtBottom) {
                isAtBottom = true
                wentTooLowDuringRep = false
                repMinTrunkAlignment = Float.MAX_VALUE
                repMaxLockoutAngle = 0f
            }
        } else if (smoothedHip > TOP_ANGLE_THRESHOLD && isAtBottom) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime >= MIN_REP_DURATION_MS) {
                isAtBottom = false
                bottomFrameCount = 0
                lastRepTime = currentTime
                lastFormScore = calculateFormScore()
                resetRepTracking()
                return true
            }
        }

        if (!isAtBottomPosition) {
            bottomFrameCount = 0
        }

        return false
    }

    override fun hadGoodForm(): Boolean = lastFormScore >= 60f

    override fun formScore(): Float = lastFormScore

    private fun calculateTrunkAlignment(
        shoulder: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        hip: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        knee: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
    ): Float {
        val shoulderToHipY = abs(shoulder.y() - hip.y())
        val hipToKneeY = abs(hip.y() - knee.y())
        if (hipToKneeY < 0.01f) return 1f
        return (shoulderToHipY / hipToKneeY).coerceIn(0f, 2f)
    }

    private fun calculateFormScore(): Float {
        var score = 100f

        if (wentTooLowDuringRep) {
            score -= SQUATTING_PENALTY
        }

        if (repMinTrunkAlignment < TRUNK_ALIGNMENT_THRESHOLD) {
            val deviation = ((TRUNK_ALIGNMENT_THRESHOLD - repMinTrunkAlignment) / 0.3f).coerceIn(0f, 1f)
            score -= deviation * BACK_ROUNDING_PENALTY_WEIGHT
        }

        if (repMaxLockoutAngle < TOP_ANGLE_THRESHOLD) {
            val deviation = ((TOP_ANGLE_THRESHOLD - repMaxLockoutAngle) / 25f).coerceIn(0f, 1f)
            score -= deviation * LOCKOUT_PENALTY_WEIGHT
        }

        return score.coerceIn(0f, 100f)
    }

    private fun resetRepTracking() {
        wentTooLowDuringRep = false
        repMinTrunkAlignment = Float.MAX_VALUE
        repMinHipAngle = Float.MAX_VALUE
        repMaxLockoutAngle = 0f
    }

    override fun reset() {
        isAtBottom = false
        bottomFrameCount = 0
        lastRepTime = 0L
        lastFormScore = 100f
        lastSmoothedHipAngle = 180f
        lastSmoothedTrunkRatio = 1f
        hipAngleSmoother.reset()
        trunkSmoother.reset()
        resetRepTracking()
    }
}
