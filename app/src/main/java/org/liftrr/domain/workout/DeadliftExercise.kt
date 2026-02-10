package org.liftrr.domain.workout

import org.liftrr.ml.PoseAnalyzer
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseLandmarks

/**
 * Deadlift exercise implementation
 * Handles rep counting and form feedback for deadlifts
 */
class DeadliftExercise : Exercise {

    // Track exercise state for rep counting
    private var isAtBottom = false
    private var bottomFrameCount = 0
    private var lastRepTime = 0L

    // Track if user went too low during current rep
    private var wentTooLowDuringRep = false

    companion object {
        private const val MIN_FRAMES_FOR_STABILITY = 2
        private const val MIN_REP_DURATION_MS = 600L
        private const val BOTTOM_ANGLE_THRESHOLD = 120f
        private const val TOP_ANGLE_THRESHOLD = 160f
        private const val SQUAT_THRESHOLD = 80f // Below this is squatting, not hinging
    }

    override fun analyzeFeedback(pose: PoseDetectionResult.Success): String {
        val leftShoulder = pose.getLandmark(PoseLandmarks.LEFT_SHOULDER)
        val rightShoulder = pose.getLandmark(PoseLandmarks.RIGHT_SHOULDER)
        val leftHip = pose.getLandmark(PoseLandmarks.LEFT_HIP)
        val rightHip = pose.getLandmark(PoseLandmarks.RIGHT_HIP)
        val leftKnee = pose.getLandmark(PoseLandmarks.LEFT_KNEE)
        val rightKnee = pose.getLandmark(PoseLandmarks.RIGHT_KNEE)

        if (leftShoulder == null || rightShoulder == null || leftHip == null ||
            rightHip == null || leftKnee == null || rightKnee == null) {
            return "Move into frame"
        }

        val hipAngle = PoseAnalyzer.calculateAngle(leftShoulder, leftHip, leftKnee)

        return when {
            // Check for squatting (bad form) - priority feedback
            hipAngle < SQUAT_THRESHOLD -> "Don't squat - hinge at hips!"
            hipAngle < 100f -> "Too bent - engage hips"
            hipAngle > 170f -> "Good lockout!"
            hipAngle > 140f -> "Almost there"
            else -> "Keep back straight"
        }
    }

    override fun updateRepCount(pose: PoseDetectionResult.Success): Boolean {
        val leftShoulder = pose.getLandmark(PoseLandmarks.LEFT_SHOULDER)
        val leftHip = pose.getLandmark(PoseLandmarks.LEFT_HIP)
        val leftKnee = pose.getLandmark(PoseLandmarks.LEFT_KNEE)

        // Check visibility of key landmarks
        if (leftShoulder == null || leftHip == null || leftKnee == null) {
            bottomFrameCount = 0
            return false
        }

        val hipAngle = PoseAnalyzer.calculateAngle(leftShoulder, leftHip, leftKnee)

        // Check if user is squatting (bad form)
        if (hipAngle < SQUAT_THRESHOLD) {
            wentTooLowDuringRep = true
        }

        // Relaxed thresholds for better detection
        val isAtBottomPosition = hipAngle < BOTTOM_ANGLE_THRESHOLD // Bent over position
        val isAtTopPosition = hipAngle > TOP_ANGLE_THRESHOLD // Standing upright

        if (isAtBottomPosition) {
            bottomFrameCount++
            // Stable bottom position achieved
            if (bottomFrameCount >= MIN_FRAMES_FOR_STABILITY && !isAtBottom) {
                isAtBottom = true
                // Reset flag for new rep when starting from bottom
                wentTooLowDuringRep = false
            }
        } else if (isAtTopPosition && isAtBottom) {
            // Rep completed: went from bottom to top
            val currentTime = System.currentTimeMillis()
            // Check minimum time between reps
            if (currentTime - lastRepTime >= MIN_REP_DURATION_MS) {
                isAtBottom = false
                bottomFrameCount = 0
                lastRepTime = currentTime

                // DON'T reset wentTooLowDuringRep here - it will be checked by hadGoodForm()
                // and reset when starting the next rep

                return true // Rep counted!
            }
        }

        // Reset counter if not at bottom
        if (!isAtBottomPosition) {
            bottomFrameCount = 0
        }

        return false
    }

    override fun hadGoodForm(): Boolean {
        // If user squatted during the rep, it's bad form
        return !wentTooLowDuringRep
    }

    override fun reset() {
        isAtBottom = false
        bottomFrameCount = 0
        lastRepTime = 0L
        wentTooLowDuringRep = false
    }
}
