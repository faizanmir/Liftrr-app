package org.liftrr.ml

/**
 * MediaPipe Pose Landmark Indices
 *
 * Constants for the 33 body landmarks detected by MediaPipe:
 * - 0-10: Face (nose, eyes, ears, mouth)
 * - 11-16: Upper body (shoulders, elbows, wrists)
 * - 17-22: Hands (pinky, index, thumb)
 * - 23-28: Lower body (hips, knees, ankles)
 * - 29-32: Feet (heel, foot index)
 *
 * Usage:
 * ```kotlin
 * val leftHip = pose.getLandmark(PoseLandmarks.LEFT_HIP)
 * val rightKnee = pose.getLandmark(PoseLandmarks.RIGHT_KNEE)
 * ```
 */
object PoseLandmarks {
    // ==================== FACE LANDMARKS (0-10) ====================

    /** Nose tip */
    const val NOSE = 0

    /** Left eye inner corner */
    const val LEFT_EYE_INNER = 1

    /** Left eye center */
    const val LEFT_EYE = 2

    /** Left eye outer corner */
    const val LEFT_EYE_OUTER = 3

    /** Right eye inner corner */
    const val RIGHT_EYE_INNER = 4

    /** Right eye center */
    const val RIGHT_EYE = 5

    /** Right eye outer corner */
    const val RIGHT_EYE_OUTER = 6

    /** Left ear */
    const val LEFT_EAR = 7

    /** Right ear */
    const val RIGHT_EAR = 8

    /** Left mouth corner */
    const val MOUTH_LEFT = 9

    /** Right mouth corner */
    const val MOUTH_RIGHT = 10

    // ==================== UPPER BODY (11-16) ====================

    /** Left shoulder */
    const val LEFT_SHOULDER = 11

    /** Right shoulder */
    const val RIGHT_SHOULDER = 12

    /** Left elbow */
    const val LEFT_ELBOW = 13

    /** Right elbow */
    const val RIGHT_ELBOW = 14

    /** Left wrist */
    const val LEFT_WRIST = 15

    /** Right wrist */
    const val RIGHT_WRIST = 16

    // ==================== HANDS (17-22) ====================

    /** Left pinky finger base */
    const val LEFT_PINKY = 17

    /** Right pinky finger base */
    const val RIGHT_PINKY = 18

    /** Left index finger base */
    const val LEFT_INDEX = 19

    /** Right index finger base */
    const val RIGHT_INDEX = 20

    /** Left thumb */
    const val LEFT_THUMB = 21

    /** Right thumb */
    const val RIGHT_THUMB = 22

    // ==================== LOWER BODY (23-28) ====================

    /** Left hip */
    const val LEFT_HIP = 23

    /** Right hip */
    const val RIGHT_HIP = 24

    /** Left knee */
    const val LEFT_KNEE = 25

    /** Right knee */
    const val RIGHT_KNEE = 26

    /** Left ankle */
    const val LEFT_ANKLE = 27

    /** Right ankle */
    const val RIGHT_ANKLE = 28

    // ==================== FEET (29-32) ====================

    /** Left heel */
    const val LEFT_HEEL = 29

    /** Right heel */
    const val RIGHT_HEEL = 30

    /** Left foot index (toe) */
    const val LEFT_FOOT_INDEX = 31

    /** Right foot index (toe) */
    const val RIGHT_FOOT_INDEX = 32
}
