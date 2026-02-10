package org.liftrr.ml

import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

/**
 * Result of pose detection
 */
sealed class PoseDetectionResult {
    /**
     * Successful pose detection with landmarks
     *
     * @property landmarks List of 33 normalized body landmarks (x, y in 0-1 range)
     * @property worldLandmarks List of 3D world coordinates for landmarks (optional)
     * @property timestamp Timestamp when the pose was detected
     * @property imageWidth Width of the input image
     * @property imageHeight Height of the input image
     */
    data class Success(
        val landmarks: List<NormalizedLandmark>,
        val worldLandmarks: List<Landmark>?,
        val timestamp: Long,
        val imageWidth: Int,
        val imageHeight: Int
    ) : PoseDetectionResult() {

        /**
         * Get landmark by index
         * Use PoseLandmarks constants for indices (e.g., PoseLandmarks.LEFT_SHOULDER)
         *
         * @param index Landmark index (0-32)
         * @return NormalizedLandmark or null if index out of bounds
         */
        fun getLandmark(index: Int): NormalizedLandmark? = landmarks.getOrNull(index)

        /**
         * Get world landmark (3D coordinates) by index
         *
         * @param index Landmark index (0-32)
         * @return Landmark or null if not available
         */
        fun getWorldLandmark(index: Int): Landmark? = worldLandmarks?.getOrNull(index)

        /**
         * Check if all specified landmarks are visible with confidence > threshold
         *
         * @param indices List of landmark indices to check
         * @param confidenceThreshold Minimum visibility confidence (default 0.5)
         * @return true if all landmarks meet confidence threshold
         */
        fun areLandmarksVisible(indices: List<Int>, confidenceThreshold: Float = 0.5f): Boolean {
            return indices.all { index ->
                getLandmark(index)?.visibility()?.let { it.get() > confidenceThreshold } ?: false
            }
        }
    }

    /**
     * No pose detected in the frame
     */
    data object NoPoseDetected : PoseDetectionResult()

    /**
     * Error occurred during pose detection
     *
     * @property message Error description
     */
    data class Error(val message: String) : PoseDetectionResult()
}
