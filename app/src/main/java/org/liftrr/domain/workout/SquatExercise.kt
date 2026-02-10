package org.liftrr.domain.workout

import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseLandmarks

/**
 * Squat exercise implementation
 * Handles rep counting and form feedback for squats
 */
class SquatExercise : Exercise {

    // Track exercise state for rep counting
    private var isAtBottom = false
    private var bottomFrameCount = 0
    private var lastRepTime = 0L

    companion object {
        private const val MIN_FRAMES_FOR_STABILITY = 2
        private const val MIN_REP_DURATION_MS = 600L
    }

    override fun analyzeFeedback(pose: PoseDetectionResult.Success): String {
        val leftHip = pose.getLandmark(PoseLandmarks.LEFT_HIP)
        val rightHip = pose.getLandmark(PoseLandmarks.RIGHT_HIP)
        val leftKnee = pose.getLandmark(PoseLandmarks.LEFT_KNEE)
        val rightKnee = pose.getLandmark(PoseLandmarks.RIGHT_KNEE)
        val leftAnkle = pose.getLandmark(PoseLandmarks.LEFT_ANKLE)

        if (leftHip == null || rightHip == null || leftKnee == null || rightKnee == null) {
            return "Move into frame"
        }

        val avgHipY = (leftHip.y() + rightHip.y()) / 2
        val avgKneeY = (leftKnee.y() + rightKnee.y()) / 2
        val isAtDepth = avgHipY > avgKneeY

        // Knee alignment check
        if (leftKnee != null && leftAnkle != null) {
            val kneeX = leftKnee.x()
            val ankleX = leftAnkle.x()
            if (kneeX < ankleX - 0.05f) {
                return "Push knees out"
            }
        }

        return if (isAtDepth) "Good depth!" else "Go lower"
    }

    override fun updateRepCount(pose: PoseDetectionResult.Success): Boolean {
        val leftHip = pose.getLandmark(PoseLandmarks.LEFT_HIP)
        val rightHip = pose.getLandmark(PoseLandmarks.RIGHT_HIP)
        val leftKnee = pose.getLandmark(PoseLandmarks.LEFT_KNEE)
        val rightKnee = pose.getLandmark(PoseLandmarks.RIGHT_KNEE)

        // Check visibility of key landmarks
        if (leftHip == null || rightHip == null || leftKnee == null || rightKnee == null) {
            bottomFrameCount = 0
            return false
        }

        val avgHipY = (leftHip.y() + rightHip.y()) / 2
        val avgKneeY = (leftKnee.y() + rightKnee.y()) / 2
        val isAtBottomPosition = avgHipY > avgKneeY

        if (isAtBottomPosition) {
            bottomFrameCount++
            // Require stable bottom position for MIN_FRAMES_FOR_STABILITY frames
            if (bottomFrameCount >= MIN_FRAMES_FOR_STABILITY && !isAtBottom) {
                isAtBottom = true
            }
        } else {
            bottomFrameCount = 0
            // Rep completed: went from bottom to top
            if (isAtBottom) {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastRep = currentTime - lastRepTime
                // Check minimum time between reps
                if (timeSinceLastRep >= MIN_REP_DURATION_MS) {
                    isAtBottom = false
                    lastRepTime = currentTime
                    return true // Rep counted!
                }
            }
        }
        return false
    }

    override fun hadGoodForm(): Boolean {
        // For now, squat form is considered good if pose quality is sufficient
        // Can add specific squat form checks here in the future
        return true
    }

    override fun reset() {
        isAtBottom = false
        bottomFrameCount = 0
        lastRepTime = 0L
    }
}
