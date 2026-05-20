package org.liftrr.ml

import androidx.compose.ui.geometry.Offset
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

fun NormalizedLandmark.toOffset(imageWidth: Int, imageHeight: Int): Offset {
    return Offset(
        x = this.x() * imageWidth,
        y = this.y() * imageHeight
    )
}

/**
 * Check if landmark is visible above threshold
 *
 * @param threshold Minimum visibility confidence (default 0.5)
 * @return true if landmark visibility >= threshold
 */
fun NormalizedLandmark.isVisible(threshold: Float = 0.5f): Boolean {
    return this.visibility().orElse(0f) >= threshold
}

/**
 * Get visibility score or default value
 *
 * @param default Default value if visibility not present
 * @return Visibility score (0.0-1.0)
 */
fun NormalizedLandmark.visibilityOrDefault(default: Float = 0f): Float {
    return this.visibility().orElse(default)
}

/**
 * Check if landmark has high confidence
 *
 * @return true if visibility >= 0.8
 */
fun NormalizedLandmark.isHighConfidence(): Boolean {
    return isVisible(threshold = 0.8f)
}
