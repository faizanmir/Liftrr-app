package org.liftrr.ml

import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

sealed class PoseDetectionResult {
    data class Success(
        val landmarks: List<NormalizedLandmark>,
        val worldLandmarks: List<Landmark>?,
        val timestamp: Long,
        val imageWidth: Int,
        val imageHeight: Int
    ) : PoseDetectionResult() {

        fun getLandmark(index: Int): NormalizedLandmark? = landmarks.getOrNull(index)

        fun getWorldLandmark(index: Int): Landmark? = worldLandmarks?.getOrNull(index)

        fun areLandmarksVisible(indices: List<Int>, confidenceThreshold: Float = 0.5f): Boolean {
            return indices.all { index ->
                getLandmark(index)?.visibility()?.let { it.get() > confidenceThreshold } ?: false
            }
        }
    }

    data object NoPoseDetected : PoseDetectionResult()

    data class Error(val message: String) : PoseDetectionResult()
}
