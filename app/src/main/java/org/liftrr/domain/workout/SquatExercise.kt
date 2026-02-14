package org.liftrr.domain.workout

import org.liftrr.ml.PoseAnalyzer
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseLandmarks

class SquatExercise : Exercise {

    private var isAtBottom = false
    private var bottomFrameCount = 0
    private var topFrameCount = 0  // Track frames at standing position
    private var lastRepTime = 0L

    private val kneeAngleSmoother = AngleSmoother(3)
    private val hipAngleSmoother = AngleSmoother(3)

    private var lastSmoothedKneeAngle = 180f
    private var lastSmoothedHipAngle = 90f
    private var usingAngleMode = false

    private var repMinKneeAngle = Float.MAX_VALUE
    private var repMaxForwardLean = 0f
    private var repMaxKneeValgus = 0f
    private var lastFormScore = 100f

    companion object {
        // Stricter stability requirements to prevent false reps
        private const val MIN_FRAMES_FOR_STABILITY = 6  // ~200ms at 30fps
        private const val MIN_REP_DURATION_MS = 1200L   // Minimum 1.2 seconds per rep
        private const val MIN_FRAMES_AT_TOP = 3         // Must hold standing position

        private const val BOTTOM_ENTRY_ANGLE = 110f
        private const val BOTTOM_EXIT_ANGLE = 130f
        private const val TOP_ANGLE_THRESHOLD = 150f    // Minimum angle at top to count

        private const val GOOD_DEPTH_ANGLE = 110f
        private const val FORWARD_LEAN_MAX = 45f
        private const val KNEE_VALGUS_THRESHOLD = 0.04f

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

        if (leftAnkle != null && rightAnkle != null) {
            val leftValgus = leftAnkle.x() - leftKnee.x()
            val rightValgus = rightKnee.x() - rightAnkle.x()
            if (maxOf(leftValgus, rightValgus) > KNEE_VALGUS_THRESHOLD) {
                return "Push knees out"
            }
        }

        if (usingAngleMode) {
            return when {
                lastSmoothedKneeAngle > 150f -> "Start your squat"
                lastSmoothedKneeAngle > BOTTOM_EXIT_ANGLE -> "Go lower"
                lastSmoothedKneeAngle <= GOOD_DEPTH_ANGLE -> "Good depth!"
                else -> "Almost there"
            }
        }

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
        val rightAnkle = pose.getLandmark(PoseLandmarks.RIGHT_ANKLE)
        val leftShoulder = pose.getLandmark(PoseLandmarks.LEFT_SHOULDER)
        val rightShoulder = pose.getLandmark(PoseLandmarks.RIGHT_SHOULDER)

        if (leftHip == null || rightHip == null || leftKnee == null || rightKnee == null) {
            bottomFrameCount = 0
            return false
        }

        val canUseAngle = leftAnkle != null || rightAnkle != null
        usingAngleMode = canUseAngle

        val isAtBottomPosition: Boolean
        val isAtTopPosition: Boolean

        if (canUseAngle) {
            val kneeAngle = BilateralAngleCalculator.calculateBilateralAngle(
                leftHip, leftKnee, leftAnkle,
                rightHip, rightKnee, rightAnkle
            )

            if (kneeAngle != null) {
                val smoothedKnee = kneeAngleSmoother.add(kneeAngle)
                lastSmoothedKneeAngle = smoothedKnee

                if (smoothedKnee < repMinKneeAngle) repMinKneeAngle = smoothedKnee
            }

            val hipAngle = BilateralAngleCalculator.calculateBilateralAngle(
                leftShoulder, leftHip, leftKnee,
                rightShoulder, rightHip, rightKnee
            )

            if (hipAngle != null) {
                val smoothedHip = hipAngleSmoother.add(hipAngle)
                lastSmoothedHipAngle = smoothedHip
                val forwardLean = (90f - smoothedHip).coerceAtLeast(0f)
                if (forwardLean > repMaxForwardLean) repMaxForwardLean = forwardLean
            }

            if (leftAnkle != null) {
                val leftValgus = leftAnkle.x() - leftKnee.x()
                if (leftValgus > repMaxKneeValgus) repMaxKneeValgus = leftValgus
            }
            if (rightAnkle != null) {
                val rightValgus = rightKnee.x() - rightAnkle.x()
                if (rightValgus > repMaxKneeValgus) repMaxKneeValgus = rightValgus
            }

            isAtBottomPosition = lastSmoothedKneeAngle < BOTTOM_ENTRY_ANGLE
            isAtTopPosition = lastSmoothedKneeAngle > TOP_ANGLE_THRESHOLD
        } else {
            val avgHipY = (leftHip.y() + rightHip.y()) / 2
            val avgKneeY = (leftKnee.y() + rightKnee.y()) / 2
            isAtBottomPosition = avgHipY > avgKneeY
            isAtTopPosition = avgHipY < avgKneeY - 0.05f  // Stricter top position requirement
        }

        if (isAtBottomPosition) {
            bottomFrameCount++
            topFrameCount = 0  // Reset top counter when at bottom
            if (bottomFrameCount >= MIN_FRAMES_FOR_STABILITY && !isAtBottom) {
                isAtBottom = true
            }
        } else if (isAtTopPosition && isAtBottom) {
            // At top position - increment top frame counter
            topFrameCount++
            bottomFrameCount = 0

            // Only count rep if held at top for required frames AND minimum duration met
            if (topFrameCount >= MIN_FRAMES_AT_TOP) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastRepTime >= MIN_REP_DURATION_MS) {
                    isAtBottom = false
                    topFrameCount = 0
                    lastRepTime = currentTime
                    lastFormScore = calculateFormScore()
                    resetRepTracking()
                    return true
                }
            }
        } else {
            // In transition zone - reset counters
            bottomFrameCount = 0
            if (!isAtTopPosition) {
                topFrameCount = 0
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

    override fun detectMovementPhase(pose: PoseDetectionResult.Success): MovementPhase {
        val landmarks = pose.landmarks
        val leftHip = landmarks.getOrNull(23)
        val leftKnee = landmarks.getOrNull(25)
        val leftAnkle = landmarks.getOrNull(27)
        val rightHip = landmarks.getOrNull(24)
        val rightKnee = landmarks.getOrNull(26)
        val rightAnkle = landmarks.getOrNull(27)

        // Use bilateral angle calculation for knee angle
        val kneeAngle = BilateralAngleCalculator.calculateBilateralAngle(
            leftHip, leftKnee, leftAnkle,
            rightHip, rightKnee, rightAnkle
        ) ?: return MovementPhase.TRANSITION

        return when {
            // Lockout/Standing position (knee angle > 160°)
            kneeAngle >= 160f -> MovementPhase.LOCKOUT

            // Bottom position (knee angle < 90° - deep squat)
            kneeAngle < 90f -> MovementPhase.BOTTOM

            // Descent phase (knee angle 120-160°, going down)
            kneeAngle in 120f..160f && !isAtBottom -> MovementPhase.DESCENT

            // Ascent phase (knee angle 90-150°, coming up)
            kneeAngle in 90f..150f && isAtBottom -> MovementPhase.ASCENT

            // Transitional positions
            else -> MovementPhase.TRANSITION
        }
    }

    override fun reset() {
        isAtBottom = false
        bottomFrameCount = 0
        topFrameCount = 0
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
