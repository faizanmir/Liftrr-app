package org.liftrr.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.MediaPipeException
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

/**
 * MediaPipe-based pose detector for real-time workout form analysis.
 *
 * This implementation uses Google's MediaPipe Pose Landmarker to detect
 * 33 body landmarks in real-time. It supports both synchronous (IMAGE mode)
 * and asynchronous (LIVE_STREAM mode) detection.
 *
 * Thread-safety: All state mutations are protected by ReentrantLock.
 * Lifecycle: Call initialize() -> detectPose*() -> stop() -> reset()
 */
@Singleton
class MediaPipePoseDetector @Inject constructor(
    @ApplicationContext private val context: Context
) : PoseDetector {

    // ─── State Management ──────────────────────────────────────────────────

    private sealed class State {
        object Uninitialized : State()
        data class Initialized(val landmarker: PoseLandmarker) : State()
        object Stopped : State()
    }

    @Volatile
    private var state: State = State.Uninitialized

    private val lock = ReentrantLock()

    // ─── Result Stream ─────────────────────────────────────────────────────

    private val _poseResults = Channel<PoseDetectionResult>(Channel.CONFLATED)
    override val poseResults: Flow<PoseDetectionResult> = _poseResults.receiveAsFlow()

    // ─── Bitmap Pool Management ────────────────────────────────────────────

    /**
     * Callback invoked when MediaPipe finishes processing a bitmap.
     * Used to safely release pooled bitmaps back to the caller.
     */
    @Volatile
    private var pendingBitmapRelease: (() -> Unit)? = null

    // ─── Configuration ─────────────────────────────────────────────────────

    companion object {
        private const val TAG = "MediaPipePoseDetector"
        private const val MODEL_FILENAME = "pose_landmarker_heavy.task"

        // Confidence thresholds for pose detection quality
        private const val MIN_POSE_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_POSE_TRACKING_CONFIDENCE = 0.5f
        private const val MIN_POSE_PRESENCE_CONFIDENCE = 0.5f
    }

    // ───────────────────────────────────────────────────────────────────────
    // Public API
    // ───────────────────────────────────────────────────────────────────────

    override fun initialize(runningMode: RunningMode) {
        lock.withLock {
            if (state is State.Initialized) {
                Log.d(TAG, "Already initialized, skipping")
                return
            }

            try {
                val landmarker = createPoseLandmarker(runningMode)
                state = State.Initialized(landmarker)
                Log.d(TAG, "PoseLandmarker initialized successfully in $runningMode mode")
            } catch (e: Exception) {
                state = State.Uninitialized
                throw IllegalStateException("Failed to initialize PoseLandmarker", e)
            }
        }
    }

    override fun stop() {
        Log.d(TAG, "Stopping pose detector")
        lock.withLock {
            if (state is State.Stopped) return

            when (val currentState = state) {
                is State.Initialized -> {
                    try {
                        releasePendingBitmap()
                        currentState.landmarker.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping detector", e)
                    }
                }
                else -> { /* Already stopped or uninitialized */ }
            }

            state = State.Stopped
        }
    }

    override fun reset() {
        Log.d(TAG, "Resetting pose detector")
        stop()
        lock.withLock {
            state = State.Uninitialized
        }
        // Clear any pending results in the channel
        _poseResults.tryReceive().getOrNull()
        Log.d(TAG, "Pose detector reset complete")
    }

    /**
     * Synchronous pose detection - runs on caller's thread.
     * Do not call from the main thread.
     *
     * @return PoseDetectionResult or null if detector is stopped
     */
    override fun detectPose(bitmap: Bitmap, timestampMs: Long): PoseDetectionResult? {
        val landmarker = getLandmarkerOrInitialize(RunningMode.IMAGE)
            ?: return PoseDetectionResult.Error("Detector is stopped")

        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detect(mpImage, ImageProcessingOptions.builder().build())
            createPoseResult(result, bitmap.width, bitmap.height, timestampMs)
        } catch (e: Exception) {
            Log.e(TAG, "Synchronous detection failed", e)
            PoseDetectionResult.Error(e.message ?: "Pose detection failed")
        }
    }

    /**
     * Asynchronous pose detection - results emitted via poseResults Flow.
     * Safe to call from any thread.
     */
    override fun detectPoseAsync(bitmap: Bitmap, timestampMs: Long) {
        detectPoseAsyncInternal(bitmap, timestampMs, onBitmapProcessed = null)
    }

    /**
     * Asynchronous pose detection with bitmap release callback.
     * Use this when working with bitmap pools to know when it's safe to recycle.
     */
    override fun detectPoseAsync(
        bitmap: Bitmap,
        timestampMs: Long,
        onBitmapProcessed: () -> Unit
    ) {
        detectPoseAsyncInternal(bitmap, timestampMs, onBitmapProcessed)
    }

    // ───────────────────────────────────────────────────────────────────────
    // Initialization Helpers
    // ───────────────────────────────────────────────────────────────────────

    private fun createPoseLandmarker(runningMode: RunningMode): PoseLandmarker {
        val baseOptions = buildBaseOptions()
        val landmarkerOptions = buildLandmarkerOptions(baseOptions, runningMode)

        return try {
            PoseLandmarker.createFromOptions(context, landmarkerOptions)
        } catch (e: MediaPipeException) {
            throw IllegalStateException(
                "Failed to create PoseLandmarker. This can happen if:\n" +
                "  - The model file ($MODEL_FILENAME) is missing or corrupted\n" +
                "  - The device doesn't support required hardware features\n" +
                "  - GPU acceleration is unavailable (currently using CPU)",
                e
            )
        }
    }

    private fun buildBaseOptions(): BaseOptions {
        return BaseOptions.builder()
            .setDelegate(Delegate.CPU) // CPU fallback for stability
            .setModelAssetPath(MODEL_FILENAME)
            .build()
    }

    private fun buildLandmarkerOptions(
        baseOptions: BaseOptions,
        runningMode: RunningMode
    ): PoseLandmarker.PoseLandmarkerOptions {
        return PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(runningMode)
            .setMinPoseDetectionConfidence(MIN_POSE_DETECTION_CONFIDENCE)
            .setMinPosePresenceConfidence(MIN_POSE_PRESENCE_CONFIDENCE)
            .setMinTrackingConfidence(MIN_POSE_TRACKING_CONFIDENCE)
            .setNumPoses(1) // Single-person workout analysis
            .setOutputSegmentationMasks(false) // Not needed for form analysis
            .apply {
                if (runningMode == RunningMode.LIVE_STREAM) {
                    configureStreamingMode()
                }
            }
            .build()
    }

    private fun PoseLandmarker.PoseLandmarkerOptions.Builder.configureStreamingMode() {
        setResultListener { result, image ->
            releasePendingBitmap()
            handlePoseResult(result, image.width, image.height)
        }
        setErrorListener { error ->
            releasePendingBitmap()
            handleError(error)
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // State Access Helpers
    // ───────────────────────────────────────────────────────────────────────

    private fun getLandmarkerOrInitialize(runningMode: RunningMode): PoseLandmarker? {
        return lock.withLock {
            when (val currentState = state) {
                is State.Initialized -> currentState.landmarker
                State.Uninitialized -> {
                    initialize(runningMode)
                    (state as? State.Initialized)?.landmarker
                }
                State.Stopped -> null
            }
        }
    }

    private fun getLandmarker(): PoseLandmarker? {
        return lock.withLock {
            (state as? State.Initialized)?.landmarker
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // Async Detection Implementation
    // ───────────────────────────────────────────────────────────────────────

    private fun detectPoseAsyncInternal(
        bitmap: Bitmap,
        timestampMs: Long,
        onBitmapProcessed: (() -> Unit)?
    ) {
        val landmarker = getLandmarker()
        if (landmarker == null) {
            handleNotInitializedError(onBitmapProcessed)
            return
        }

        try {
            val mpImage = BitmapImageBuilder(bitmap).build()

            lock.withLock {
                // Release previous bitmap if MediaPipe dropped that frame
                releasePendingBitmap()
                pendingBitmapRelease = onBitmapProcessed
            }

            landmarker.detectAsync(mpImage, timestampMs)
        } catch (e: Exception) {
            lock.withLock {
                releasePendingBitmap()
            }
            Log.e(TAG, "Async detection failed", e)
            handleError(e)
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // Result Handling
    // ───────────────────────────────────────────────────────────────────────

    private fun handlePoseResult(
        result: PoseLandmarkerResult,
        imageWidth: Int,
        imageHeight: Int
    ) {
        val detectionResult = createPoseResult(
            result,
            imageWidth,
            imageHeight,
            timestampMs = System.currentTimeMillis()
        )
        _poseResults.trySend(detectionResult)
    }

    private fun createPoseResult(
        result: PoseLandmarkerResult,
        imageWidth: Int,
        imageHeight: Int,
        timestampMs: Long
    ): PoseDetectionResult {
        return if (result.landmarks().isNotEmpty()) {
            PoseDetectionResult.Success(
                landmarks = result.landmarks()[0],
                worldLandmarks = result.worldLandmarks().getOrNull(0),
                timestamp = timestampMs,
                imageWidth = imageWidth,
                imageHeight = imageHeight
            )
        } else {
            PoseDetectionResult.NoPoseDetected
        }
    }

    private fun handleError(error: Throwable) {
        Log.e(TAG, "Pose detection error: ${error.message}", error)
        _poseResults.trySend(
            PoseDetectionResult.Error(
                error.message ?: "Unknown error: ${error.javaClass.simpleName}"
            )
        )
    }

    private fun handleNotInitializedError(onBitmapProcessed: (() -> Unit)?) {
        when (state) {
            State.Uninitialized -> {
                Log.e(TAG, "Not initialized. Call initialize() first.")
                _poseResults.trySend(PoseDetectionResult.Error("PoseDetector not initialized"))
            }
            State.Stopped -> {
                Log.d(TAG, "Detector is stopped, ignoring detection request")
            }
            is State.Initialized -> { /* Should not happen */ }
        }
        onBitmapProcessed?.invoke()
    }

    // ───────────────────────────────────────────────────────────────────────
    // Bitmap Pool Management
    // ───────────────────────────────────────────────────────────────────────

    private fun releasePendingBitmap() {
        pendingBitmapRelease?.invoke()
        pendingBitmapRelease = null
    }
}
