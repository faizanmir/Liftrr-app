package org.liftrr.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseLandmarks
import org.liftrr.ml.isVisible

/**
 * Real-time skeleton overlay that draws detected pose landmarks and
 * connections on top of the camera preview.
 */
@Composable
fun PoseSkeletonOverlay(
    pose: PoseDetectionResult,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val successPose = pose as? PoseDetectionResult.Success ?: return

    val leftColor = MaterialTheme.colorScheme.primary
    val rightColor = MaterialTheme.colorScheme.tertiary
    val centerColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier.fillMaxSize()) {
        val landmarks = successPose.landmarks
        if (landmarks.size < 33) return@Canvas

        val lineWidth = 3.dp.toPx()
        val dotRadius = 5.dp.toPx()
        val visibilityThreshold = 0.5f

        fun landmarkOffset(index: Int): Offset? {
            val lm = landmarks.getOrNull(index) ?: return null
            if (!lm.isVisible(visibilityThreshold)) return null
            val x = if (isFrontCamera) (1f - lm.x()) * size.width else lm.x() * size.width
            val y = lm.y() * size.height
            return Offset(x, y)
        }

        fun drawConnection(from: Int, to: Int, color: Color) {
            val start = landmarkOffset(from) ?: return
            val end = landmarkOffset(to) ?: return
            drawLine(
                color = color.copy(alpha = 0.7f),
                start = start,
                end = end,
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )
        }

        fun drawLandmarkDot(index: Int, color: Color) {
            val pos = landmarkOffset(index) ?: return
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = dotRadius * 1.6f,
                center = pos
            )
            drawCircle(
                color = color,
                radius = dotRadius,
                center = pos
            )
        }

        // === Draw connections ===

        // Torso (center color)
        drawConnection(PoseLandmarks.LEFT_SHOULDER, PoseLandmarks.RIGHT_SHOULDER, centerColor)
        drawConnection(PoseLandmarks.LEFT_HIP, PoseLandmarks.RIGHT_HIP, centerColor)
        drawConnection(PoseLandmarks.LEFT_SHOULDER, PoseLandmarks.LEFT_HIP, leftColor)
        drawConnection(PoseLandmarks.RIGHT_SHOULDER, PoseLandmarks.RIGHT_HIP, rightColor)

        // Left arm
        drawConnection(PoseLandmarks.LEFT_SHOULDER, PoseLandmarks.LEFT_ELBOW, leftColor)
        drawConnection(PoseLandmarks.LEFT_ELBOW, PoseLandmarks.LEFT_WRIST, leftColor)

        // Right arm
        drawConnection(PoseLandmarks.RIGHT_SHOULDER, PoseLandmarks.RIGHT_ELBOW, rightColor)
        drawConnection(PoseLandmarks.RIGHT_ELBOW, PoseLandmarks.RIGHT_WRIST, rightColor)

        // Left hand
        drawConnection(PoseLandmarks.LEFT_WRIST, PoseLandmarks.LEFT_PINKY, leftColor)
        drawConnection(PoseLandmarks.LEFT_WRIST, PoseLandmarks.LEFT_INDEX, leftColor)
        drawConnection(PoseLandmarks.LEFT_WRIST, PoseLandmarks.LEFT_THUMB, leftColor)

        // Right hand
        drawConnection(PoseLandmarks.RIGHT_WRIST, PoseLandmarks.RIGHT_PINKY, rightColor)
        drawConnection(PoseLandmarks.RIGHT_WRIST, PoseLandmarks.RIGHT_INDEX, rightColor)
        drawConnection(PoseLandmarks.RIGHT_WRIST, PoseLandmarks.RIGHT_THUMB, rightColor)

        // Left leg
        drawConnection(PoseLandmarks.LEFT_HIP, PoseLandmarks.LEFT_KNEE, leftColor)
        drawConnection(PoseLandmarks.LEFT_KNEE, PoseLandmarks.LEFT_ANKLE, leftColor)
        drawConnection(PoseLandmarks.LEFT_ANKLE, PoseLandmarks.LEFT_HEEL, leftColor)
        drawConnection(PoseLandmarks.LEFT_ANKLE, PoseLandmarks.LEFT_FOOT_INDEX, leftColor)

        // Right leg
        drawConnection(PoseLandmarks.RIGHT_HIP, PoseLandmarks.RIGHT_KNEE, rightColor)
        drawConnection(PoseLandmarks.RIGHT_KNEE, PoseLandmarks.RIGHT_ANKLE, rightColor)
        drawConnection(PoseLandmarks.RIGHT_ANKLE, PoseLandmarks.RIGHT_HEEL, rightColor)
        drawConnection(PoseLandmarks.RIGHT_ANKLE, PoseLandmarks.RIGHT_FOOT_INDEX, rightColor)

        // === Draw landmark dots (on top of lines) ===

        // Body joints (left side)
        val leftJoints = listOf(
            PoseLandmarks.LEFT_SHOULDER, PoseLandmarks.LEFT_ELBOW,
            PoseLandmarks.LEFT_WRIST, PoseLandmarks.LEFT_HIP,
            PoseLandmarks.LEFT_KNEE, PoseLandmarks.LEFT_ANKLE
        )
        leftJoints.forEach { drawLandmarkDot(it, leftColor) }

        // Body joints (right side)
        val rightJoints = listOf(
            PoseLandmarks.RIGHT_SHOULDER, PoseLandmarks.RIGHT_ELBOW,
            PoseLandmarks.RIGHT_WRIST, PoseLandmarks.RIGHT_HIP,
            PoseLandmarks.RIGHT_KNEE, PoseLandmarks.RIGHT_ANKLE
        )
        rightJoints.forEach { drawLandmarkDot(it, rightColor) }
    }
}
