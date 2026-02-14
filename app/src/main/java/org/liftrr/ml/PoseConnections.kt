package org.liftrr.ml

import androidx.compose.ui.graphics.Color

object PoseConnections {

    val SKELETON_LINES = listOf(
        PoseLandmarks.NOSE to PoseLandmarks.LEFT_EYE_INNER,
        PoseLandmarks.LEFT_EYE_INNER to PoseLandmarks.LEFT_EYE,
        PoseLandmarks.LEFT_EYE to PoseLandmarks.LEFT_EYE_OUTER,
        PoseLandmarks.LEFT_EYE_OUTER to PoseLandmarks.LEFT_EAR,
        PoseLandmarks.NOSE to PoseLandmarks.RIGHT_EYE_INNER,
        PoseLandmarks.RIGHT_EYE_INNER to PoseLandmarks.RIGHT_EYE,
        PoseLandmarks.RIGHT_EYE to PoseLandmarks.RIGHT_EYE_OUTER,
        PoseLandmarks.RIGHT_EYE_OUTER to PoseLandmarks.RIGHT_EAR,
        PoseLandmarks.MOUTH_LEFT to PoseLandmarks.MOUTH_RIGHT,

        PoseLandmarks.LEFT_SHOULDER to PoseLandmarks.RIGHT_SHOULDER,
        PoseLandmarks.LEFT_SHOULDER to PoseLandmarks.LEFT_HIP,
        PoseLandmarks.RIGHT_SHOULDER to PoseLandmarks.RIGHT_HIP,
        PoseLandmarks.LEFT_HIP to PoseLandmarks.RIGHT_HIP,

        PoseLandmarks.LEFT_SHOULDER to PoseLandmarks.LEFT_ELBOW,
        PoseLandmarks.LEFT_ELBOW to PoseLandmarks.LEFT_WRIST,
        PoseLandmarks.LEFT_WRIST to PoseLandmarks.LEFT_PINKY,
        PoseLandmarks.LEFT_WRIST to PoseLandmarks.LEFT_INDEX,
        PoseLandmarks.LEFT_WRIST to PoseLandmarks.LEFT_THUMB,
        PoseLandmarks.LEFT_PINKY to PoseLandmarks.LEFT_INDEX,

        PoseLandmarks.RIGHT_SHOULDER to PoseLandmarks.RIGHT_ELBOW,
        PoseLandmarks.RIGHT_ELBOW to PoseLandmarks.RIGHT_WRIST,
        PoseLandmarks.RIGHT_WRIST to PoseLandmarks.RIGHT_PINKY,
        PoseLandmarks.RIGHT_WRIST to PoseLandmarks.RIGHT_INDEX,
        PoseLandmarks.RIGHT_WRIST to PoseLandmarks.RIGHT_THUMB,
        PoseLandmarks.RIGHT_PINKY to PoseLandmarks.RIGHT_INDEX,

        PoseLandmarks.LEFT_HIP to PoseLandmarks.LEFT_KNEE,
        PoseLandmarks.LEFT_KNEE to PoseLandmarks.LEFT_ANKLE,
        PoseLandmarks.LEFT_ANKLE to PoseLandmarks.LEFT_HEEL,
        PoseLandmarks.LEFT_ANKLE to PoseLandmarks.LEFT_FOOT_INDEX,
        PoseLandmarks.LEFT_HEEL to PoseLandmarks.LEFT_FOOT_INDEX,

        PoseLandmarks.RIGHT_HIP to PoseLandmarks.RIGHT_KNEE,
        PoseLandmarks.RIGHT_KNEE to PoseLandmarks.RIGHT_ANKLE,
        PoseLandmarks.RIGHT_ANKLE to PoseLandmarks.RIGHT_HEEL,
        PoseLandmarks.RIGHT_ANKLE to PoseLandmarks.RIGHT_FOOT_INDEX,
        PoseLandmarks.RIGHT_HEEL to PoseLandmarks.RIGHT_FOOT_INDEX
    )

    /**
     * Colors for rendering different body parts
     */
    object Colors {
        val FACE = Color(0xFF00BCD4)           // Cyan
        val TORSO = Color(0xFFFF6B35)          // Orange
        val LEFT_ARM = Color(0xFF7CB342)       // Green
        val RIGHT_ARM = Color(0xFF7CB342)      // Green
        val LEFT_LEG = Color(0xFF1565C0)       // Blue
        val RIGHT_LEG = Color(0xFF1565C0)      // Blue
        val HANDS = Color(0xFFE91E63)          // Pink
        val FEET = Color(0xFF9E9E9E)           // Gray
        val DEFAULT = Color(0xFF00D9FF)        // Bright cyan (fallback)
    }

    /**
     * Get color for specific landmark connection
     *
     * @param startIdx Starting landmark index
     * @param endIdx Ending landmark index
     * @return Color for the connection
     */
    fun getConnectionColor(startIdx: Int, endIdx: Int): Color {
        return when {
            // Face connections
            startIdx in 0..10 && endIdx in 0..10 -> Colors.FACE

            // Torso connections
            (startIdx == PoseLandmarks.LEFT_SHOULDER && endIdx == PoseLandmarks.RIGHT_SHOULDER) ||
            (startIdx == PoseLandmarks.LEFT_HIP && endIdx == PoseLandmarks.RIGHT_HIP) ||
            (startIdx == PoseLandmarks.LEFT_SHOULDER && endIdx == PoseLandmarks.LEFT_HIP) ||
            (startIdx == PoseLandmarks.RIGHT_SHOULDER && endIdx == PoseLandmarks.RIGHT_HIP) -> Colors.TORSO

            // Left arm
            startIdx in 11..15 && endIdx in 11..22 && startIdx % 2 == 1 -> Colors.LEFT_ARM

            // Right arm
            startIdx in 12..16 && endIdx in 12..22 && startIdx % 2 == 0 -> Colors.RIGHT_ARM

            // Left leg
            startIdx in 23..31 && endIdx in 23..31 && startIdx % 2 == 1 -> Colors.LEFT_LEG

            // Right leg
            startIdx in 24..32 && endIdx in 24..32 && startIdx % 2 == 0 -> Colors.RIGHT_LEG

            // Hands
            startIdx in 17..22 || endIdx in 17..22 -> Colors.HANDS

            // Feet
            startIdx in 29..32 || endIdx in 29..32 -> Colors.FEET

            else -> Colors.DEFAULT
        }
    }
}
