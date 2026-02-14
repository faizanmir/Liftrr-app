package org.liftrr.ml

import android.graphics.Bitmap
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.flow.Flow

interface PoseDetector {

    val poseResults: Flow<PoseDetectionResult>

    fun initialize(runningMode: RunningMode = RunningMode.LIVE_STREAM)

    fun stop()

    fun reset()

    fun detectPose(bitmap: Bitmap, timestampMs: Long): PoseDetectionResult?

    fun detectPoseAsync(bitmap: Bitmap, timestampMs: Long)

    fun detectPoseAsync(bitmap: Bitmap, timestampMs: Long, onBitmapProcessed: () -> Unit) {
        detectPoseAsync(bitmap, timestampMs)
        onBitmapProcessed()
    }
}
