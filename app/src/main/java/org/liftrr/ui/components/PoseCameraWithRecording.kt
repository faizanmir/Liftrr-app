package org.liftrr.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.asExecutor
import org.liftrr.domain.video.VideoRecordingManager
import org.liftrr.utils.BitmapPool
import org.liftrr.utils.DefaultDispatcherProvider
import org.liftrr.utils.DispatcherProvider
import java.io.File

/**
 * Camera preview with pose detection and video recording.
 * @param onFrameCaptured Called with (bitmap, timestamp, release). Caller MUST invoke release()
 *                        only when the bitmap is no longer referenced (e.g. after MediaPipe finishes).
 */
@Composable
fun PoseCameraWithRecording(
    onFrameCaptured: (bitmap: Bitmap, timestamp: Long, release: () -> Unit) -> Unit,
    isRecording: Boolean,
    onRecordingStarted: (File) -> Unit = {},
    onRecordingStopped: (String) -> Unit = {},
    onRecordingError: (String) -> Unit = {},
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val recordingManager = remember { VideoRecordingManager(context) }
    val recordingState by recordingManager.recordingState.collectAsState()

    // Lazily initialized bitmap pool - dimensions set from first frame's output
    val outputPool = remember { mutableStateOf<BitmapPool?>(null) }

    val isProcessingFrame = remember { mutableStateOf(false) }
    val lastProcessedTime = remember { mutableStateOf(0L) }
    val minFrameInterval = 33L

    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            recordingManager.cleanup()
            outputPool.value?.clear()
        }
    }

    LaunchedEffect(isRecording, videoCapture) {
        if (isRecording) {
            val capture = videoCapture
            if (capture != null) {
                recordingManager.startRecording(
                    videoCapture = capture,
                    onStarted = onRecordingStarted,
                    onStopped = onRecordingStopped,
                    onError = onRecordingError
                )
            }
        } else {
            recordingManager.stopRecording()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                dispatchers.io.asExecutor()
                            ) { imageProxy ->
                                processImageProxyWithSkipping(
                                    imageProxy,
                                    onFrameCaptured,
                                    outputPool,
                                    isProcessingFrame,
                                    lastProcessedTime,
                                    minFrameInterval
                                )
                            }
                        }

                    val recorder = Recorder.Builder()
                        .setQualitySelector(
                            QualitySelector.from(
                                Quality.HD,
                                FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
                            )
                        )
                        .build()
                    val videoCaptureUseCase = VideoCapture.withOutput(recorder)
                    videoCapture = videoCaptureUseCase

                    try {
                        // Don't use unbindAll() as it affects other camera composables in the backstack
                        // Instead, just bind - CameraX will handle unbinding previous use cases for this lifecycle owner
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis,
                            videoCaptureUseCase
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onRecordingError("Failed to bind camera: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun processImageProxyWithSkipping(
    imageProxy: ImageProxy,
    onFrameCaptured: (bitmap: Bitmap, timestamp: Long, release: () -> Unit) -> Unit,
    outputPool: MutableState<BitmapPool?>,
    isProcessingFrame: MutableState<Boolean>,
    lastProcessedTime: MutableState<Long>,
    minFrameInterval: Long
) {
    try {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastFrame = currentTime - lastProcessedTime.value

        if (isProcessingFrame.value || timeSinceLastFrame < minFrameInterval) {
            return
        }

        isProcessingFrame.value = true
        lastProcessedTime.value = currentTime

        val bitmap = imageProxyToBitmap(imageProxy, outputPool)
        if (bitmap != null) {
            val pool = outputPool.value
            onFrameCaptured(bitmap, currentTime) {
                // Return to pool when MediaPipe is done (instead of waiting for GC)
                if (pool != null) {
                    pool.release(bitmap)
                } else if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
                isProcessingFrame.value = false
            }
        } else {
            isProcessingFrame.value = false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        isProcessingFrame.value = false
    } finally {
        imageProxy.close()
    }
}

/**
 * Convert ImageProxy to a pooled Bitmap.
 * Intermediate bitmaps are recycled eagerly. The output bitmap comes from the pool
 * and must be returned via pool.release() when no longer needed.
 */
private fun imageProxyToBitmap(imageProxy: ImageProxy, outputPool: MutableState<BitmapPool?>): Bitmap? {
    return try {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * imageProxy.width

        // Temporary buffer bitmap (always recycled within this function)
        val bufferBitmap = createBitmap(imageProxy.width + rowPadding / pixelStride, imageProxy.height)
        buffer.rewind()
        bufferBitmap.copyPixelsFromBuffer(buffer)

        val croppedBitmap = if (rowPadding > 0) {
            val cropped = Bitmap.createBitmap(bufferBitmap, 0, 0, imageProxy.width, imageProxy.height)
            bufferBitmap.recycle()
            cropped
        } else {
            bufferBitmap
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees.toFloat()

        // Calculate output dimensions (may swap w/h for 90/270 rotation)
        val outWidth: Int
        val outHeight: Int
        if (rotationDegrees == 90f || rotationDegrees == 270f) {
            outWidth = croppedBitmap.height
            outHeight = croppedBitmap.width
        } else {
            outWidth = croppedBitmap.width
            outHeight = croppedBitmap.height
        }

        // Initialize pool lazily from first frame's output dimensions
        if (outputPool.value == null) {
            outputPool.value = BitmapPool(
                capacity = 4,
                width = outWidth,
                height = outHeight
            )
        }

        val pool = outputPool.value!!
        val output = pool.acquire()

        if (rotationDegrees == 0f) {
            // No rotation - direct copy into pooled bitmap
            val canvas = Canvas(output)
            canvas.drawBitmap(croppedBitmap, 0f, 0f, null)
        } else {
            // Rotate source into pooled bitmap via Canvas
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees)
            val rect = RectF(0f, 0f, croppedBitmap.width.toFloat(), croppedBitmap.height.toFloat())
            matrix.mapRect(rect)
            matrix.postTranslate(-rect.left, -rect.top)

            val canvas = Canvas(output)
            canvas.drawBitmap(croppedBitmap, matrix, null)
        }

        croppedBitmap.recycle()
        output
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
