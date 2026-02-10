package org.liftrr.domain.workout

import org.liftrr.ml.PoseAnalyzer
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseLandmarks

/**
 * Squat exercise with angle-based depth detection, hysteresis,
 * knee valgus check, forward lean check, and weighted penalty scoring.
 *
 * Falls back to Y-position depth if ankles are not visible.
 */
class SquatExercise : Exercise {

    private var isAtBottom = false
    private var bottomFrameCount = 0
    private var lastRepTime = 0L

    // Angle smoothers — only used in updateRepCount to avoid double-feed
    private val kneeAngleSmoother = AngleSmoother(3)
    private val hipAngleSmoother = AngleSmoother(3)

    // Cached smoothed values from updateRepCount for analyzeFeedback to read
    private var lastSmoothedKneeAngle = 180f
    private var lastSmoothedHipAngle = 90f
    private var usingAngleMode = false

    // Per-rep form tracking
    private var repMinKneeAngle = Float.MAX_VALUE
    private var repMaxForwardLean = 0f
    private var repMaxKneeValgus = 0f
    private var lastFormScore = 100f

    companion object {
        private const val MIN_FRAMES_FOR_STABILITY = 2
        private const val MIN_REP_DURATION_MS = 600L

        // Hysteresis thresholds for knee angle
        private const val BOTTOM_ENTRY_ANGLE = 110f   // Must go below this to enter bottom
        private const val BOTTOM_EXIT_ANGLE = 130f    // Must go above this to exit bottom

        // Form thresholds
        private const val GOOD_DEPTH_ANGLE = 110f
        private const val FORWARD_LEAN_MAX = 45f
        private const val KNEE_VALGUS_THRESHOLD = 0.04f

        // Penalty weights (out of 100)
        private const val DEPTH_PENALTY_WEIGHT = 35f
        private const val FORWARD_LEAN_PENALTY_WEIGHT = 25f
        private const val KNEE_VALGUS_PENALTY_WEIGHT = 20f
    }

    override fun analyzeFeedback(pose: PoseDetectionResult.Success): String {
        val leftHip = pose.getLandmark(PoseLandmarks.LEFT_HIP)
        val rightHip = pose.getLandmark(PoseLandmarks.RIGHT_HIP)
        val leftKnee = pose.getLandmark(PoseLandmarks.LEFT_KNEE)
        val rightKnee = pose.getLandmark(PoseLandmarks.RIGHT_KNEE)
        val leftAnkle = pose.getLandmark(PoseLandmarks.LEFT_ANKLE)
        val rightAnkle = pose.getLandmark(PoseLandmarks.RIGHT_ANKLE)

        if (leftHip == null || rightHip == null || leftKnee == null || rightKnee == null) {
            return "Move into frame"
        }

        // Knee valgus check (only when ankles visible)
        if (leftAnkle != null && rightAnkle != null) {
            val leftValgus = leftAnkle.x() - leftKnee.x()
            val rightValgus = rightKnee.x() - rightAnkle.x()
            if (maxOf(leftValgus, rightValgus) > KNEE_VALGUS_THRESHOLD) {
                return "Push knees out"
            }
        }

        // Use cached smoothed values from updateRepCount (avoids double-feed)
        if (usingAngleMode) {
            return when {
                lastSmoothedKneeAngle > 150f -> "Start your squat"
                lastSmoothedKneeAngle > BOTTOM_EXIT_ANGLE -> "Go lower"
                lastSmoothedKneeAngle <= GOOD_DEPTH_ANGLE -> "Good depth!"
                else -> "Almost there"
            }
        }

        // Fallback: Y-position feedback
        val avgHipY = (leftHip.y() + rightHip.y()) / 2
        val avgKneeY = (leftKnee.y() + rightKnee.y()) / 2
        return if (avgHipY > avgKneeY) "Good depth!" else "Go lower"
    }

    override fun updateRepCount(pose: PoseDetectionResult.Success): Boolean {
        val leftHip = pose.getLandmark(PoseLandmarks.LEFT_HIP)
        val rightHip = pose.getLandmark(PoseLandmarks.RIGHT_HIP)
        val leftKnee = pose.getLandmark(PoseLandmarks.LEFT_KNEE)
        val rightKnee = pose.getLandmark(PoseLandmarks.RIGHT_KNEE)
        val leftAnkle = pose.getLandmark(PoseLandmarks.LEFT_ANKLE)
        val leftShoulder = pose.getLandmark(PoseLandmarks.LEFT_SHOULDER)

        if (leftHip == null || rightHip == null || leftKnee == null || rightKnee == null) {
            bottomFrameCount = 0
            return false
        }

        // Determine if we can use angle-based detection
        val canUseAngle = leftAnkle != null
        usingAngleMode = canUseAngle

        val isAtBottomPosition: Boolean
        val isAtTopPosition: Boolean

        if (canUseAngle) {
            val kneeAngle = PoseAnalyzer.calculateAngle(leftHip, leftKnee, leftAnkle)
            val smoothedKnee = kneeAngleSmoother.add(kneeAngle)
            lastSmoothedKneeAngle = smoothedKnee

            // Track form metrics
            if (smoothedKnee < repMinKneeAngle) repMinKneeAngle = smoothedKnee

            // Forward lean tracking
            if (leftShoulder != null) {
                val hipAngle = PoseAnalyzer.calculateAngle(leftShoulder, leftHip, leftKnee)
                val smoothedHip = hipAngleSmoother.add(hipAngle)
                lastSmoothedHipAngle = smoothedHip
                val forwardLean = (90f - smoothedHip).coerceAtLeast(0f)
                if (forwardLean > repMaxForwardLean) repMaxForwardLean = forwardLean
            }

            // Knee valgus tracking
            val rightAnkle = pose.getLandmark(PoseLandmarks.RIGHT_ANKLE)
            if (rightAnkle != null) {
                val leftValgus = leftAnkle.x() - leftKnee.x()
                val rightValgus = rightKnee.x() - rightAnkle.x()
                val maxValgus = maxOf(leftValgus, rightValgus, 0f)
                if (maxValgus > repMaxKneeValgus) repMaxKneeValgus = maxValgus
            }

            isAtBottomPosition = smoothedKnee < BOTTOM_ENTRY_ANGLE
            isAtTopPosition = smoothedKnee > BOTTOM_EXIT_ANGLE
        } else {
            // Fallback: Y-position based detection (original method)
            val avgHipY = (leftHip.y() + rightHip.y()) / 2
            val avgKneeY = (leftKnee.y() + rightKnee.y()) / 2
            isAtBottomPosition = avgHipY > avgKneeY
            isAtTopPosition = avgHipY < avgKneeY - 0.03f
        }

        // State machine
        if (isAtBottomPosition) {
            bottomFrameCount++
            if (bottomFrameCount >= MIN_FRAMES_FOR_STABILITY && !isAtBottom) {
                isAtBottom = true
            }
        } else {
            bottomFrameCount = 0
            if (isAtTopPosition && isAtBottom) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastRepTime >= MIN_REP_DURATION_MS) {
                    isAtBottom = false
                    lastRepTime = currentTime
                    lastFormScore = calculateFormScore()
                    resetRepTracking()
                    return true
                }
            }
        }

        return false
    }

    override fun hadGoodForm(): Boolean = lastFormScore >= 60f

    override fun formScore(): Float = lastFormScore

    private fun calculateFormScore(): Float {
        var score = 100f

        if (usingAngleMode) {
            if (repMinKneeAngle > GOOD_DEPTH_ANGLE) {
                val depthDeviation = ((repMinKneeAngle - GOOD_DEPTH_ANGLE) / 40f).coerceIn(0f, 1f)
                score -= depthDeviation * DEPTH_PENALTY_WEIGHT
            }

            if (repMaxForwardLean > FORWARD_LEAN_MAX) {
                val leanDeviation = ((repMaxForwardLean - FORWARD_LEAN_MAX) / 30f).coerceIn(0f, 1f)
                score -= leanDeviation * FORWARD_LEAN_PENALTY_WEIGHT
            }

            if (repMaxKneeValgus > KNEE_VALGUS_THRESHOLD) {
                val valgusDeviation = ((repMaxKneeValgus - KNEE_VALGUS_THRESHOLD) / 0.06f).coerceIn(0f, 1f)
                score -= valgusDeviation * KNEE_VALGUS_PENALTY_WEIGHT
            }
        }

        return score.coerceIn(0f, 100f)
    }

    private fun resetRepTracking() {
        repMinKneeAngle = Float.MAX_VALUE
        repMaxForwardLean = 0f
        repMaxKneeValgus = 0f
    }

    override fun reset() {
        isAtBottom = false
        bottomFrameCount = 0
        lastRepTime = 0L
        lastFormScore = 100f
        usingAngleMode = false
        lastSmoothedKneeAngle = 180f
        lastSmoothedHipAngle = 90f
        kneeAngleSmoother.reset()
        hipAngleSmoother.reset()
        resetRepTracking()
    }
}
