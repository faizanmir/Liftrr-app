package org.liftrr.ui.screens.workout

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.liftrr.ml.CameraAngle
import org.liftrr.ml.ExerciseType
import org.liftrr.ml.FramingFeedback
import org.liftrr.ml.LightingQuality
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseDetector
import org.liftrr.ml.PoseQuality
import org.liftrr.ml.PoseQualityAnalyzer
import org.liftrr.utils.DispatcherProvider
import javax.inject.Inject

data class ReadinessState(
    val isFramingGood: Boolean = false,
    val isCameraAngleCorrect: Boolean = false,
    val areLandmarksVisible: Boolean = false,
    val isLightingAdequate: Boolean = false,
    val poseQuality: PoseQuality? = null,
    val isDetectorReady: Boolean = false
) {
    val allChecksPassed: Boolean
        get() = isFramingGood && isCameraAngleCorrect
                && areLandmarksVisible && isLightingAdequate

    val passedCount: Int
        get() = listOf(
            isFramingGood, isCameraAngleCorrect,
            areLandmarksVisible, isLightingAdequate
        ).count { it }
}

@HiltViewModel
class CameraReadinessViewModel @Inject constructor(
    private val poseDetector: PoseDetector,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _readinessState = MutableStateFlow(ReadinessState())
    val readinessState: StateFlow<ReadinessState> = _readinessState.asStateFlow()

    private val _latestPose = MutableStateFlow<PoseDetectionResult>(PoseDetectionResult.NoPoseDetected)
    val latestPose: StateFlow<PoseDetectionResult> = _latestPose.asStateFlow()

    private var exerciseType: ExerciseType = ExerciseType.SQUAT
    private var collectionJob: Job? = null

    fun setExerciseType(type: ExerciseType) {
        exerciseType = type
    }

    fun startReadinessCheck() {
        viewModelScope.launch {
            withContext(dispatchers.io) {
                poseDetector.initialize()
            }
            _readinessState.update { it.copy(isDetectorReady = true) }

            collectionJob = poseDetector.poseResults
                .onEach { result -> processResult(result) }
                .launchIn(viewModelScope)
        }
    }

    private fun processResult(result: PoseDetectionResult) {
        _latestPose.value = result
        when (result) {
            is PoseDetectionResult.Success -> {
                val quality = PoseQualityAnalyzer.analyzeExerciseSpecificQuality(
                    result.landmarks, exerciseType
                )
                _readinessState.update {
                    it.copy(
                        isFramingGood = quality.framingFeedback == FramingFeedback.WELL_FRAMED
                                || quality.framingFeedback == FramingFeedback.GOOD_DISTANCE,
                        isCameraAngleCorrect = quality.cameraAngle == CameraAngle.SIDE_VIEW,
                        areLandmarksVisible = quality.isFullBodyVisible,
                        isLightingAdequate = quality.lightingQuality != LightingQuality.TOO_DARK,
                        poseQuality = quality
                    )
                }
            }
            is PoseDetectionResult.NoPoseDetected -> {
                _readinessState.update {
                    it.copy(
                        isFramingGood = false,
                        isCameraAngleCorrect = false,
                        areLandmarksVisible = false,
                        poseQuality = null
                    )
                }
            }
            is PoseDetectionResult.Error -> { /* keep last state */ }
        }
    }

    fun processFrame(bitmap: Bitmap, timestamp: Long, release: () -> Unit) {
        poseDetector.detectPoseAsync(bitmap, timestamp, release)
    }

    fun stopReadinessCheck() {
        collectionJob?.cancel()
        collectionJob = null
    }

    override fun onCleared() {
        super.onCleared()
        collectionJob?.cancel()
    }
}
