package org.liftrr.domain.workout

import org.liftrr.ml.PoseAnalyzer
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseLandmarks
import kotlin.math.abs

class BenchPressExercise : Exercise {

    private var isAtBottom = false
    private var bottomFrameCount = 0
    private var lastRepTime = 0L

    private val elbowAngleSmoother = AngleSmoother(3)

    private var lastSmoothedElbowAngle = 180f

    private var repMinElbowAngle = Float.MAX_VALUE
    private var repMaxElbowFlare = 0f
    private var repMaxLockoutAngle = 0f
    private var lastFormScore = 100f

    companion object {
        private const val MIN_FRAMES_FOR_STABILITY = 2
        private const val MIN_REP_DURATION_MS = 600L

        private const val BOTTOM_ENTRY_ANGLE = 110f
        private const val BOTTOM_EXIT_ANGLE = 130f
        private const val TOP_ANGLE_THRESHOLD = 155f

        private const val GOOD_DEPTH_ANGLE = 110f
        private const val ELBOW_FLARE_MIN = 30f
        private const val ELBOW_FLARE_MAX = 75f
        private const val ELBOW_FLARE_THRESHOLD = 0.12f

        private const val DEPTH_PENALTY_WEIGHT = 30f
        private const val ELBOW_FLARE_PENALTY_WEIGHT = 25f
        private const val LOCKOUT_PENALTY_WEIGHT = 25f
    }

    override fun analyzeFeedback(pose: PoseDetectionResult.Success): String {
        val leftWrist = pose.getLandmark(PoseLandmarks.LEFT_WRIST)
        val rightWrist = pose.getLandmark(PoseLandmarks.RIGHT_WRIST)
        val leftElbow = pose.getLandmark(PoseLandmarks.LEFT_ELBOW)
        val rightElbow = pose.getLandmark(PoseLandmarks.RIGHT_ELBOW)
        val leftShoulder = pose.getLandmark(PoseLandmarks.LEFT_SHOULDER)
        val rightShoulder = pose.getLandmark(PoseLandmarks.RIGHT_SHOULDER)

        if (leftWrist == null || rightWrist == null || leftElbow == null ||
            rightElbow == null || leftShoulder == null || rightShoulder == null
        ) {
            return "Position yourself on bench"
        }

        val shoulderWidth = abs(leftShoulder.x() - rightShoulder.x())
        val elbowWidth = abs(leftElbow.x() - rightElbow.x())
        val flareRatio = if (shoulderWidth > 0.01f) elbowWidth / shoulderWidth else 1f

        if (flareRatio > 1.8f && lastSmoothedElbowAngle < BOTTOM_EXIT_ANGLE) {
            return "Tuck elbows in"
        }

        if (flareRatio < 0.7f && lastSmoothedElbowAngle < BOTTOM_EXIT_ANGLE) {
            return "Flare elbows out slightly"
        }

        return when {
            lastSmoothedElbowAngle > TOP_ANGLE_THRESHOLD -> "Good lockout!"
            lastSmoothedElbowAngle > BOTTOM_EXIT_ANGLE -> "Lower the bar"
            lastSmoothedElbowAngle <= GOOD_DEPTH_ANGLE -> "Touch chest"
            else -> "Go deeper"
        }
    }

    override fun updateRepCount(pose: PoseDetectionResult.Success): Boolean {
        val leftWrist = pose.getLandmark(PoseLandmarks.LEFT_WRIST)
        val leftElbow = pose.getLandmark(PoseLandmarks.LEFT_ELBOW)
        val rightWrist = pose.getLandmark(PoseLandmarks.RIGHT_WRIST)
        val rightElbow = pose.getLandmark(PoseLandmarks.RIGHT_ELBOW)
        val leftShoulder = pose.getLandmark(PoseLandmarks.LEFT_SHOULDER)
        val rightShoulder = pose.getLandmark(PoseLandmarks.RIGHT_SHOULDER)

        val elbowAngle = BilateralAngleCalculator.calculateBilateralAngle(
            leftShoulder, leftElbow, leftWrist,
            rightShoulder, rightElbow, rightWrist
        )

        if (elbowAngle == null) {
            bottomFrameCount = 0
            return false
        }

        val smoothedElbow = elbowAngleSmoother.add(elbowAngle)
        lastSmoothedElbowAngle = smoothedElbow

        if (smoothedElbow < repMinElbowAngle) repMinElbowAngle = smoothedElbow
        if (smoothedElbow > repMaxLockoutAngle) repMaxLockoutAngle = smoothedElbow

        if (leftElbow != null && rightElbow != null && leftShoulder != null && rightShoulder != null) {
            val shoulderWidth = abs(leftShoulder.x() - rightShoulder.x())
            val elbowWidth = abs(leftElbow.x() - rightElbow.x())
            val flareRatio = if (shoulderWidth > 0.01f) elbowWidth / shoulderWidth else 1f
            if (flareRatio > repMaxElbowFlare) repMaxElbowFlare = flareRatio
        }

        val isAtBottomPosition = smoothedElbow < BOTTOM_ENTRY_ANGLE

        if (isAtBottomPosition) {
            bottomFrameCount++
            if (bottomFrameCount >= MIN_FRAMES_FOR_STABILITY && !isAtBottom) {
                isAtBottom = true
            }
        } else if (smoothedElbow > BOTTOM_EXIT_ANGLE && isAtBottom) {
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

    private fun calculateFormScore(): Float {
        var score = 100f

        if (repMinElbowAngle > GOOD_DEPTH_ANGLE) {
            val depthDeviation = ((repMinElbowAngle - GOOD_DEPTH_ANGLE) / 40f).coerceIn(0f, 1f)
            score -= depthDeviation * DEPTH_PENALTY_WEIGHT
        }

        val idealFlare = 1.3f
        val flareDev = abs(repMaxElbowFlare - idealFlare)
        if (flareDev > 0.3f) {
            val flareDeviation = ((flareDev - 0.3f) / 0.8f).coerceIn(0f, 1f)
            score -= flareDeviation * ELBOW_FLARE_PENALTY_WEIGHT
        }

        if (repMaxLockoutAngle < TOP_ANGLE_THRESHOLD) {
            val deviation = ((TOP_ANGLE_THRESHOLD - repMaxLockoutAngle) / 25f).coerceIn(0f, 1f)
            score -= deviation * LOCKOUT_PENALTY_WEIGHT
        }

        return score.coerceIn(0f, 100f)
    }

    private fun resetRepTracking() {
        repMinElbowAngle = Float.MAX_VALUE
        repMaxElbowFlare = 0f
        repMaxLockoutAngle = 0f
    }

    override fun reset() {
        isAtBottom = false
        bottomFrameCount = 0
        lastRepTime = 0L
        lastFormScore = 100f
        lastSmoothedElbowAngle = 180f
        elbowAngleSmoother.reset()
        resetRepTracking()
    }
}
