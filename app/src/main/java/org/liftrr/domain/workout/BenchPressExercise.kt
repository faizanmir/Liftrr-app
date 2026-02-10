package org.liftrr.domain.workout

import org.liftrr.ml.PoseAnalyzer
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseLandmarks

/**
 * Bench Press exercise implementation
 * Handles rep counting and form feedback for bench press
 */
class BenchPressExercise : Exercise {

    // Track exercise state for rep counting
    private var isAtBottom = false
    private var bottomFrameCount = 0
    private var lastRepTime = 0L

    companion object {
        private const val MIN_FRAMES_FOR_STABILITY = 2
        private const val MIN_REP_DURATION_MS = 600L
    }

    override fun analyzeFeedback(pose: PoseDetectionResult.Success): String {
        val leftWrist = pose.getLandmark(PoseLandmarks.LEFT_WRIST)
        val rightWrist = pose.getLandmark(PoseLandmarks.RIGHT_WRIST)
        val leftElbow = pose.getLandmark(PoseLandmarks.LEFT_ELBOW)
        val leftShoulder = pose.getLandmark(PoseLandmarks.LEFT_SHOULDER)
        val rightShoulder = pose.getLandmark(PoseLandmarks.RIGHT_SHOULDER)

        if (leftWrist == null || rightWrist == null || leftShoulder == null || rightShoulder == null) {
            return "Position yourself on bench"
        }

        val avgWristY = (leftWrist.y() + rightWrist.y()) / 2
        val avgShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2
        val isAtBottomPosition = avgWristY >= avgShoulderY - 0.05f

        // Check elbow angle for proper form
        if (leftElbow != null) {
            val elbowAngle = PoseAnalyzer.calculateAngle(leftWrist, leftElbow, leftShoulder)
            if (elbowAngle < 45f) {
                return "Flare elbows out"
            }
        }

        return if (isAtBottomPosition) "Touch chest" else "Good extension!"
    }

    override fun updateRepCount(pose: PoseDetectionResult.Success): Boolean {
        val leftWrist = pose.getLandmark(PoseLandmarks.LEFT_WRIST)
        val rightWrist = pose.getLandmark(PoseLandmarks.RIGHT_WRIST)
        val leftShoulder = pose.getLandmark(PoseLandmarks.LEFT_SHOULDER)
        val rightShoulder = pose.getLandmark(PoseLandmarks.RIGHT_SHOULDER)

        // Check visibility of key landmarks
        if (leftWrist == null || rightWrist == null || leftShoulder == null || rightShoulder == null) {
            bottomFrameCount = 0
            return false
        }

        val avgWristY = (leftWrist.y() + rightWrist.y()) / 2
        val avgShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2
        val isAtBottomPosition = avgWristY >= avgShoulderY - 0.05f // Bar at chest

        if (isAtBottomPosition) {
            bottomFrameCount++
            // Stable bottom position achieved
            if (bottomFrameCount >= MIN_FRAMES_FOR_STABILITY && !isAtBottom) {
                isAtBottom = true
            }
        } else {
            bottomFrameCount = 0
            // Rep completed: went from bottom to top (full lockout)
            if (isAtBottom) {
                val currentTime = System.currentTimeMillis()
                // Check minimum time between reps
                if (currentTime - lastRepTime >= MIN_REP_DURATION_MS) {
                    isAtBottom = false
                    lastRepTime = currentTime
                    return true // Rep counted!
                }
            }
        }
        return false
    }

    override fun hadGoodForm(): Boolean {
        // For now, bench press form is considered good if pose quality is sufficient
        // Can add specific bench press form checks here in the future
        return true
    }

    override fun reset() {
        isAtBottom = false
        bottomFrameCount = 0
        lastRepTime = 0L
    }
}
