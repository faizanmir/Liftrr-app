package org.liftrr.ml

import android.graphics.Bitmap
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.flow.Flow

/**
 * Interface for pose detection implementations
 * Provides lifecycle management and pose detection capabilities
 */
interface PoseDetector {

    /**
     * Flow of pose detection results
     * Emits results in real-time when using LIVE_STREAM mode
     */
    val poseResults: Flow<PoseDetectionResult>

    /**
     * Initialize the pose detector
     *
     * @param runningMode LIVE_STREAM for real-time camera feed, IMAGE for single frame detection
     * @throws IllegalStateException if initialization fails
     */
    fun initialize(runningMode: RunningMode = RunningMode.LIVE_STREAM)

    /**
     * Stop the pose detector and release resources
     * Closes the underlying detector but keeps the flow active
     */
    fun stop()

    /**
     * Reset/clear the pose detector state
     * Clears internal state and allows re-initialization
     * Use this when navigating away and coming back to detection screen
     */
    fun reset()

    /**
     * Detect pose landmarks in a single image (synchronous)
     *
     * @param bitmap Input image
     * @param timestampMs Timestamp for tracking
     * @return PoseDetectionResult with landmarks or error
     */
    fun detectPose(bitmap: Bitmap, timestampMs: Long): PoseDetectionResult?

    /**
     * Process video frame for real-time pose detection (asynchronous)
     * Results will be emitted to the poseResults Flow
     *
     * @param bitmap Video frame
     * @param timestampMs Frame timestamp (must be monotonically increasing)
     */
    fun detectPoseAsync(bitmap: Bitmap, timestampMs: Long)

    /**
     * Process video frame with a callback invoked when the bitmap is no longer referenced.
     * Used by bitmap pooling to safely return bitmaps after MediaPipe finishes reading.
     */
    fun detectPoseAsync(bitmap: Bitmap, timestampMs: Long, onBitmapProcessed: () -> Unit) {
        detectPoseAsync(bitmap, timestampMs)
        onBitmapProcessed()
    }
}
