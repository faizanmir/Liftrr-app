package org.liftrr.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPipePoseDetector @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PoseDetector {

    @Volatile
    private var poseLandmarker: PoseLandmarker? = null

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isStopped = false

    private val _poseResults = Channel<PoseDetectionResult>(Channel.CONFLATED)
    override val poseResults: Flow<PoseDetectionResult> = _poseResults.receiveAsFlow()

    private val lock = Any()

    // Bitmap pool release callback - invoked when MediaPipe is done reading pixels
    @Volatile
    private var pendingBitmapRelease: (() -> Unit)? = null

    companion object {
        private const val MODEL_FILENAME = "pose_landmarker_heavy.task"
        private const val MIN_POSE_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_POSE_TRACKING_CONFIDENCE = 0.5f
        private const val MIN_POSE_PRESENCE_CONFIDENCE = 0.5f
    }

    override fun initialize(runningMode: RunningMode) {
        synchronized(lock) {
            // Prevent re-initialization if already initialized
            if (isInitialized && poseLandmarker != null) {
                Log.d("MediaPipePoseDetector", "Already initialized, skipping")
                return
            }

            try {
                val baseOptions = BaseOptions.builder()
                    .setDelegate(Delegate.GPU)
                    .setModelAssetPath(MODEL_FILENAME)
                    .build()

                val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(runningMode)
                    .setMinPoseDetectionConfidence(MIN_POSE_DETECTION_CONFIDENCE)
                    .setMinPosePresenceConfidence(MIN_POSE_PRESENCE_CONFIDENCE)
                    .setMinTrackingConfidence(MIN_POSE_TRACKING_CONFIDENCE)
                    .setNumPoses(1) // Track single person for workout analysis
                    .setOutputSegmentationMasks(false) // We don't need segmentation
                    .apply {
                        if (runningMode == RunningMode.LIVE_STREAM) {
                            setResultListener { result, image ->
                                // MediaPipe is done reading the bitmap - safe to release
                                pendingBitmapRelease?.invoke()
                                pendingBitmapRelease = null
                                handlePoseResult(result, image.width, image.height)
                            }
                            setErrorListener { error ->
                                pendingBitmapRelease?.invoke()
                                pendingBitmapRelease = null
                                handleError(error)
                            }
                        }
                    }
                    .build()

                poseLandmarker = PoseLandmarker.createFromOptions(context, options)
                isInitialized = true
                isStopped = false
                Log.d("MediaPipePoseDetector", "PoseLandmarker initialized successfully")
            } catch (e: IllegalStateException) {
                isInitialized = false
                throw e
            } catch (e: Exception) {
                isInitialized = false
                Log.e("MediaPipePoseDetector", "Failed to initialize MediaPipe", e)
                throw IllegalStateException(
                    "Failed to initialize PoseLandmarker: ${e.message}\n" +
                    "Cause: ${e.cause?.message ?: "Unknown"}",
                    e
                )
            }
        }
    }

    override fun stop() {
        Log.d("MediaPipePoseDetector", "Stopping pose detector")
        synchronized(lock) {
            try {
                isStopped = true
                pendingBitmapRelease?.invoke()
                pendingBitmapRelease = null
                poseLandmarker?.close()
            } catch (e: Exception) {
                Log.e("MediaPipePoseDetector", "Error stopping detector", e)
            } finally {
                poseLandmarker = null
                isInitialized = false
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun reset() {
        Log.d("MediaPipePoseDetector", "Resetting pose detector")

        synchronized(lock) {
            try {
                isStopped = true
                pendingBitmapRelease?.invoke()
                pendingBitmapRelease = null
                poseLandmarker?.close()
            } catch (e: Exception) {
                Log.e("MediaPipePoseDetector", "Error stopping detector during reset", e)
            } finally {
                poseLandmarker = null
                isInitialized = false
            }

            // Clear any pending results in the channel
            try {
                // Cancel and recreate the channel to clear any stale data
                while (!_poseResults.isEmpty) {
                    _poseResults.tryReceive()
                }
            } catch (e: Exception) {
                Log.e("MediaPipePoseDetector", "Error clearing channel", e)
            }
        }

        Log.d("MediaPipePoseDetector", "Pose detector reset complete")
    }

    override fun detectPose(bitmap: Bitmap, timestampMs: Long): PoseDetectionResult? {
        synchronized(lock) {
            if (isStopped) {
                return PoseDetectionResult.Error("Detector is stopped")
            }

            val landmarker = poseLandmarker ?: run {
                initialize(RunningMode.IMAGE)
                poseLandmarker!!
            }

            return try {
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result = landmarker.detect(
                    mpImage,
                    ImageProcessingOptions.builder().build()
                )

                if (result.landmarks().isNotEmpty()) {
                    PoseDetectionResult.Success(
                        landmarks = result.landmarks()[0],
                        worldLandmarks = result.worldLandmarks().getOrNull(0),
                        timestamp = timestampMs,
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height
                    )
                } else {
                    PoseDetectionResult.NoPoseDetected
                }
            } catch (e: Exception) {
                PoseDetectionResult.Error(e.message ?: "Pose detection failed")
            }
        }
    }

    override fun detectPoseAsync(bitmap: Bitmap, timestampMs: Long) {
        detectPoseAsyncInternal(bitmap, timestampMs, onBitmapProcessed = null)
    }

    override fun detectPoseAsync(bitmap: Bitmap, timestampMs: Long, onBitmapProcessed: () -> Unit) {
        detectPoseAsyncInternal(bitmap, timestampMs, onBitmapProcessed)
    }

    private fun detectPoseAsyncInternal(
        bitmap: Bitmap,
        timestampMs: Long,
        onBitmapProcessed: (() -> Unit)?
    ) {
        if (isStopped) {
            onBitmapProcessed?.invoke()
            return
        }

        synchronized(lock) {
            if (isStopped) {
                onBitmapProcessed?.invoke()
                return
            }

            val landmarker = poseLandmarker ?: run {
                Log.e("MediaPipePoseDetector", "Not initialized. Call initialize() first.")
                _poseResults.trySend(
                    PoseDetectionResult.Error("PoseDetector not initialized")
                )
                onBitmapProcessed?.invoke()
                return
            }

            try {
                // Release previous bitmap if MediaPipe dropped that frame
                pendingBitmapRelease?.invoke()
                pendingBitmapRelease = onBitmapProcessed

                val mpImage = BitmapImageBuilder(bitmap).build()
                landmarker.detectAsync(mpImage, timestampMs)
            } catch (e: Exception) {
                pendingBitmapRelease?.invoke()
                pendingBitmapRelease = null
                Log.e("MediaPipePoseDetector", "Error during pose detection", e)
                handleError(e)
            }
        }
    }

    private fun handlePoseResult(
        result: PoseLandmarkerResult,
        imageWidth: Int,
        imageHeight: Int
    ) {
        val detectionResult = if (result.landmarks().isNotEmpty()) {
            PoseDetectionResult.Success(
                landmarks = result.landmarks()[0],
                worldLandmarks = result.worldLandmarks().getOrNull(0),
                timestamp = System.currentTimeMillis(),
                imageWidth = imageWidth,
                imageHeight = imageHeight
            )
        } else {
            PoseDetectionResult.NoPoseDetected
        }

        _poseResults.trySend(detectionResult)
    }

    private fun handleError(error: Throwable) {
        Log.e("MediaPipePoseDetector", "Pose detection error", error)
        _poseResults.trySend(
            PoseDetectionResult.Error(
                error.message ?: "Unknown error: ${error.javaClass.simpleName}"
            )
        )
    }
}
