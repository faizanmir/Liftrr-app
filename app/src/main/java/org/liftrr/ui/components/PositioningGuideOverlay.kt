package org.liftrr.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.liftrr.ml.ExerciseType

/**
 * Semi-transparent positioning guide overlay that shows a target zone
 * and key landmark positions for the current exercise type.
 * Fades in/out via [isVisible].
 */
@Composable
fun PositioningGuideOverlay(
    exerciseType: ExerciseType,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "guide_alpha"
    )

    val guideColor = MaterialTheme.colorScheme.primary

    if (alpha > 0f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawPositioningGuide(exerciseType, guideColor, alpha)
        }
    }
}

private fun DrawScope.drawPositioningGuide(
    exerciseType: ExerciseType,
    color: Color,
    alpha: Float
) {
    val guide = getGuideForExercise(exerciseType, size)
    val dashEffect = PathEffect.dashPathEffect(
        floatArrayOf(16.dp.toPx(), 8.dp.toPx()),
        0f
    )

    // Target rectangle fill
    drawRoundRect(
        color = color.copy(alpha = 0.08f * alpha),
        topLeft = guide.rectTopLeft,
        size = guide.rectSize,
        cornerRadius = CornerRadius(16.dp.toPx())
    )

    // Target rectangle dashed border
    drawRoundRect(
        color = color.copy(alpha = 0.35f * alpha),
        topLeft = guide.rectTopLeft,
        size = guide.rectSize,
        cornerRadius = CornerRadius(16.dp.toPx()),
        style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = dashEffect
        )
    )

    // Landmark dots with labels
    guide.landmarks.forEach { landmark ->
        // Outer ring
        drawCircle(
            color = color.copy(alpha = 0.25f * alpha),
            radius = 12.dp.toPx(),
            center = landmark.position
        )
        // Inner dot
        drawCircle(
            color = color.copy(alpha = 0.5f * alpha),
            radius = 5.dp.toPx(),
            center = landmark.position
        )
    }

    // Connecting lines between landmarks
    if (guide.landmarks.size >= 2) {
        for (i in 0 until guide.landmarks.size - 1) {
            drawLine(
                color = color.copy(alpha = 0.2f * alpha),
                start = guide.landmarks[i].position,
                end = guide.landmarks[i + 1].position,
                strokeWidth = 1.5f.dp.toPx(),
                pathEffect = dashEffect
            )
        }
    }
}

private data class LandmarkGuide(
    val position: Offset,
    val label: String
)

private data class ExerciseGuide(
    val rectTopLeft: Offset,
    val rectSize: Size,
    val landmarks: List<LandmarkGuide>
)

private fun getGuideForExercise(type: ExerciseType, canvasSize: Size): ExerciseGuide {
    return when (type) {
        ExerciseType.SQUAT -> {
            // Full body side view
            val rectW = canvasSize.width * 0.45f
            val rectH = canvasSize.height * 0.75f
            val left = (canvasSize.width - rectW) / 2f
            val top = canvasSize.height * 0.08f
            val cx = canvasSize.width * 0.5f
            ExerciseGuide(
                rectTopLeft = Offset(left, top),
                rectSize = Size(rectW, rectH),
                landmarks = listOf(
                    LandmarkGuide(Offset(cx, canvasSize.height * 0.18f), "Shoulder"),
                    LandmarkGuide(Offset(cx - canvasSize.width * 0.02f, canvasSize.height * 0.42f), "Hip"),
                    LandmarkGuide(Offset(cx, canvasSize.height * 0.60f), "Knee"),
                    LandmarkGuide(Offset(cx + canvasSize.width * 0.01f, canvasSize.height * 0.78f), "Ankle")
                )
            )
        }
        ExerciseType.DEADLIFT -> {
            // Full body side view (same layout as squat)
            val rectW = canvasSize.width * 0.45f
            val rectH = canvasSize.height * 0.75f
            val left = (canvasSize.width - rectW) / 2f
            val top = canvasSize.height * 0.08f
            val cx = canvasSize.width * 0.5f
            ExerciseGuide(
                rectTopLeft = Offset(left, top),
                rectSize = Size(rectW, rectH),
                landmarks = listOf(
                    LandmarkGuide(Offset(cx, canvasSize.height * 0.18f), "Shoulder"),
                    LandmarkGuide(Offset(cx - canvasSize.width * 0.02f, canvasSize.height * 0.42f), "Hip"),
                    LandmarkGuide(Offset(cx, canvasSize.height * 0.60f), "Knee"),
                    LandmarkGuide(Offset(cx + canvasSize.width * 0.01f, canvasSize.height * 0.78f), "Ankle")
                )
            )
        }
        ExerciseType.BENCH_PRESS -> {
            // Upper body focus, slightly wider
            val rectW = canvasSize.width * 0.55f
            val rectH = canvasSize.height * 0.50f
            val left = (canvasSize.width - rectW) / 2f
            val top = canvasSize.height * 0.12f
            val cx = canvasSize.width * 0.5f
            ExerciseGuide(
                rectTopLeft = Offset(left, top),
                rectSize = Size(rectW, rectH),
                landmarks = listOf(
                    LandmarkGuide(Offset(cx - canvasSize.width * 0.05f, canvasSize.height * 0.22f), "Shoulder"),
                    LandmarkGuide(Offset(cx + canvasSize.width * 0.06f, canvasSize.height * 0.28f), "Elbow"),
                    LandmarkGuide(Offset(cx + canvasSize.width * 0.12f, canvasSize.height * 0.20f), "Wrist"),
                    LandmarkGuide(Offset(cx - canvasSize.width * 0.08f, canvasSize.height * 0.45f), "Hip")
                )
            )
        }
    }
}
