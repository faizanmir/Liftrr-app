package org.liftrr.domain.video

import android.content.Context
import android.os.Environment
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages video recording lifecycle and state
 *
 * Separates business logic from UI concerns.
 * Handles file creation, recording start/stop, and state management.
 */
class VideoRecordingManager(private val context: Context) {

    private var activeRecording: Recording? = null
    private var currentVideoFile: File? = null

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    /**
     * Start video recording
     */
    fun startRecording(
        videoCapture: VideoCapture<Recorder>,
        onStarted: (File) -> Unit = {},
        onStopped: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (activeRecording != null) {
            onError("Recording already in progress")
            return
        }

        try {
            val videoFile = createVideoFile()
            currentVideoFile = videoFile

            val outputOptions = FileOutputOptions.Builder(videoFile).build()

            activeRecording = videoCapture.output
                .prepareRecording(context, outputOptions)
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    handleRecordEvent(recordEvent, videoFile, onStarted, onStopped, onError)
                }

            _recordingState.value = RecordingState.Starting
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(e.message ?: "Failed to start recording")
            onError(e.message ?: "Failed to start recording")
        }
    }

    /**
     * Stop video recording
     */
    fun stopRecording() {
        activeRecording?.stop()
        _recordingState.value = RecordingState.Stopping
    }

    /**
     * Handle recording events from CameraX
     */
    private fun handleRecordEvent(
        event: VideoRecordEvent,
        videoFile: File,
        onStarted: (File) -> Unit,
        onStopped: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        when (event) {
            is VideoRecordEvent.Start -> {
                _recordingState.value = RecordingState.Recording(videoFile)
                onStarted(videoFile)
            }
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    _recordingState.value = RecordingState.Error(
                        "Recording error: ${event.error}"
                    )
                    onError("Recording error: ${event.error}")
                } else {
                    _recordingState.value = RecordingState.Completed(videoFile.absolutePath)
                    onStopped(videoFile.absolutePath)
                }
                activeRecording = null
                currentVideoFile = null
            }
            is VideoRecordEvent.Status -> {
                // Optional: handle status updates (duration, file size, etc.)
            }
            is VideoRecordEvent.Pause -> {
                _recordingState.value = RecordingState.Paused
            }
            is VideoRecordEvent.Resume -> {
                _recordingState.value = RecordingState.Recording(videoFile)
            }
        }
    }

    /**
     * Create video file in app-specific storage
     */
    private fun createVideoFile(): File {
        val videoDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: throw IllegalStateException("Cannot access external storage")

        if (!videoDir.exists()) {
            videoDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "LIFTRR_${timestamp}.mp4"

        return File(videoDir, fileName)
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        activeRecording?.stop()
        activeRecording = null
        currentVideoFile = null
        _recordingState.value = RecordingState.Idle
    }
}

/**
 * Recording state sealed class
 */
sealed class RecordingState {
    data object Idle : RecordingState()
    data object Starting : RecordingState()
    data class Recording(val file: File) : RecordingState()
    data object Paused : RecordingState()
    data object Stopping : RecordingState()
    data class Completed(val uri: String) : RecordingState()
    data class Error(val message: String) : RecordingState()
}
