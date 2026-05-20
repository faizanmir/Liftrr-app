package org.liftrr.ml

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.acos
import kotlin.math.sqrt

object PoseAnalyzer {

    fun calculateAngle(
        point1: NormalizedLandmark,
        point2: NormalizedLandmark,
        point3: NormalizedLandmark
    ): Float {
        val vector1X = point1.x() - point2.x()
        val vector1Y = point1.y() - point2.y()
        val vector2X = point3.x() - point2.x()
        val vector2Y = point3.y() - point2.y()

        val dotProduct = vector1X * vector2X + vector1Y * vector2Y
        val magnitude1 = sqrt(vector1X * vector1X + vector1Y * vector1Y)
        val magnitude2 = sqrt(vector2X * vector2X + vector2Y * vector2Y)

        val cosine = dotProduct / (magnitude1 * magnitude2)
        return Math.toDegrees(acos(cosine.coerceIn(-1f, 1f)).toDouble()).toFloat()
    }

    /**
     * Check if a squat is at proper depth
     *
     * Squat depth is determined by comparing hip position to knee position.
     * In image coordinates (Y increases downward), hip Y > knee Y means at depth.
     *
     * @param leftHip Left hip landmark
     * @param rightHip Right hip landmark
     * @param leftKnee Left knee landmark
     * @param rightKnee Right knee landmark
     * @return true if hips are below parallel (proper squat depth)
     */
    fun isSquatAtDepth(
        leftHip: NormalizedLandmark,
        rightHip: NormalizedLandmark,
        leftKnee: NormalizedLandmark,
        rightKnee: NormalizedLandmark
    ): Boolean {
        val avgHipY = (leftHip.y() + rightHip.y()) / 2
        val avgKneeY = (leftKnee.y() + rightKnee.y()) / 2
        return avgHipY > avgKneeY // Y increases downward in image coordinates
    }

    /**
     * Detect if performing a bench press (lying down)
     *
     * Checks if shoulders and hips are roughly horizontal, indicating
     * the person is lying on a bench.
     *
     * @param leftShoulder Left shoulder landmark
     * @param rightShoulder Right shoulder landmark
     * @param leftHip Left hip landmark
     * @param rightHip Right hip landmark
     * @return true if shoulders and hips are level (lying down position)
     */
    fun isLyingDown(
        leftShoulder: NormalizedLandmark,
        rightShoulder: NormalizedLandmark,
        leftHip: NormalizedLandmark,
        rightHip: NormalizedLandmark
    ): Boolean {
        val shoulderYDiff = kotlin.math.abs(leftShoulder.y() - rightShoulder.y())
        val hipYDiff = kotlin.math.abs(leftHip.y() - rightHip.y())
        return shoulderYDiff < 0.1f && hipYDiff < 0.1f // Shoulders and hips are level
    }

    /**
     * Check if bench press bar is at bottom position (near chest)
     *
     * Bar position tracked by wrists. Wrists at or below shoulder level
     * indicates bar is at chest.
     *
     * @param leftWrist Left wrist landmark
     * @param rightWrist Right wrist landmark
     * @param leftShoulder Left shoulder landmark
     * @param rightShoulder Right shoulder landmark
     * @return true if bar is at chest position
     */
    fun isBenchPressAtBottom(
        leftWrist: NormalizedLandmark,
        rightWrist: NormalizedLandmark,
        leftShoulder: NormalizedLandmark,
        rightShoulder: NormalizedLandmark
    ): Boolean {
        val avgWristY = (leftWrist.y() + rightWrist.y()) / 2
        val avgShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2
        // Wrists should be at or below shoulder level when bar is at chest
        return avgWristY >= avgShoulderY - 0.05f
    }

    /**
     * Detect if performing a deadlift (standing with hip hinge)
     *
     * Checks if person is upright with visible hips and shoulders.
     * Shoulders should be above hips in standing/bent over position.
     *
     * @param leftShoulder Left shoulder landmark
     * @param rightShoulder Right shoulder landmark
     * @param leftHip Left hip landmark
     * @param rightHip Right hip landmark
     * @return true if in deadlift stance (shoulders above hips)
     */
    fun isDeadliftStance(
        leftShoulder: NormalizedLandmark,
        rightShoulder: NormalizedLandmark,
        leftHip: NormalizedLandmark,
        rightHip: NormalizedLandmark
    ): Boolean {
        val avgShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2
        val avgHipY = (leftHip.y() + rightHip.y()) / 2
        // Shoulders should be above hips (standing/bent over position)
        return avgShoulderY < avgHipY
    }

    /**
     * Check if deadlift is at bottom position (bent over)
     *
     * Hip should be bent and shoulders forward. Hip angle less than 120°
     * indicates bent over loading position.
     *
     * @param leftShoulder Left shoulder landmark
     * @param rightShoulder Right shoulder landmark
     * @param leftHip Left hip landmark
     * @param rightHip Right hip landmark
     * @param leftKnee Left knee landmark
     * @param rightKnee Right knee landmark
     * @return true if at bottom (bent over) position
     */
    fun isDeadliftAtBottom(
        leftShoulder: NormalizedLandmark,
        rightShoulder: NormalizedLandmark,
        leftHip: NormalizedLandmark,
        rightHip: NormalizedLandmark,
        leftKnee: NormalizedLandmark,
        rightKnee: NormalizedLandmark
    ): Boolean {
        val hipAngle = calculateAngle(leftShoulder, leftHip, leftKnee)
        // Hip angle should be less than 120 degrees when bent over
        return hipAngle < 120f
    }

    /**
     * Check if deadlift is at top position (standing upright)
     *
     * Hips should be extended. Hip angle greater than 160° indicates
     * full lockout at the top.
     *
     * @param leftShoulder Left shoulder landmark
     * @param rightShoulder Right shoulder landmark
     * @param leftHip Left hip landmark
     * @param rightHip Right hip landmark
     * @param leftKnee Left knee landmark
     * @param rightKnee Right knee landmark
     * @return true if at top (standing upright) position
     */
    fun isDeadliftAtTop(
        leftShoulder: NormalizedLandmark,
        rightShoulder: NormalizedLandmark,
        leftHip: NormalizedLandmark,
        rightHip: NormalizedLandmark,
        leftKnee: NormalizedLandmark,
        rightKnee: NormalizedLandmark
    ): Boolean {
        val hipAngle = calculateAngle(leftShoulder, leftHip, leftKnee)
        // Hip angle should be greater than 160 degrees when standing upright
        return hipAngle > 160f
    }
}
