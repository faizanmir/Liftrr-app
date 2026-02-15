package org.liftrr.ui.screens.workout

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import org.liftrr.data.models.RepDataDto
import org.liftrr.data.models.WorkoutSessionEntity
import org.liftrr.data.repository.WorkoutRepository
import org.liftrr.domain.analytics.WorkoutAnalyzer
import org.liftrr.domain.analytics.WorkoutReport
import org.liftrr.domain.analytics.WorkoutSession
import org.liftrr.domain.workout.MovementPhase
import org.liftrr.domain.workout.RepData
import org.liftrr.domain.workout.WorkoutEngine
import org.liftrr.domain.workout.WorkoutReportHolder
import org.liftrr.ml.ExerciseType
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseDetector
import org.liftrr.ml.PoseQuality
import org.liftrr.ui.screens.session.WorkoutMode
import org.liftrr.utils.DispatcherProvider
import org.liftrr.utils.KeyFrameCapture
import javax.inject.Inject

/**
 * UI State for the workout screen
 */
data class WorkoutUiState(
    val mode: WorkoutMode,
    val exerciseType: ExerciseType = ExerciseType.SQUAT,
    val isRecording: Boolean = false,
    val isPoseDetectionInitializing: Boolean = false,
    val currentPose: PoseDetectionResult = PoseDetectionResult.NoPoseDetected,
    val repCount: Int = 0,
    val goodReps: Int = 0,
    val badReps: Int = 0,
    val reps: List<RepData> = emptyList(),
    val formFeedback: String = "",
    val poseQualityScore: Float = 0f,
    val poseQuality: PoseQuality? = null
)

/**
 * ViewModel for workout screen
 * Follows Single Responsibility Principle - only handles UI state management
 * Business logic delegated to WorkoutEngine
 */
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    val poseDetector: PoseDetector,
    private val workoutReportHolder: WorkoutReportHolder,
    private val workoutRepository: WorkoutRepository,
    val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState(mode = WorkoutMode.SENSOR_AND_CAMERA))
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val workoutEngine = WorkoutEngine()
    private val keyFrameCapture = KeyFrameCapture(context)

    private var lastWorkoutReport: WorkoutReport? = null
    private var videoUri: String? = null
    private var weight: Float? = null
    private var isPoseDetectorInitialized = false

    private var latestFrameBitmap: Bitmap? = null
    private var latestFrameTimestamp: Long = 0L
    private val bitmapLock = Any()

    private val capturedPhasesThisRep = mutableSetOf<MovementPhase>()
    private var lastRepNumber = 0

    fun preInitializePoseDetector() {
        if (isPoseDetectorInitialized) return
        viewModelScope.launch {
            try {
                withContext(dispatchers.io) { poseDetector.initialize() }
                isPoseDetectorInitialized = true
            } catch (e: Exception) {
                android.util.Log.e("WorkoutViewModel", "Initialization failed", e)
            }
        }
    }

    fun observePoseDetection() {
        viewModelScope.launch {
            try {
                if (!isPoseDetectorInitialized) {
                    _uiState.update { it.copy(isPoseDetectionInitializing = true) }
                    withContext(dispatchers.io) { poseDetector.initialize() }
                    isPoseDetectorInitialized = true
                    _uiState.update { it.copy(isPoseDetectionInitializing = false) }
                }

                poseDetector.poseResults
                    .onEach { result ->
                        // Move processing to Default dispatcher to keep UI responsive
                        withContext(dispatchers.default) {
                            val workoutState = workoutEngine.processPoseResult(result)
                            val repStats = workoutEngine.getRepStats()
                            val currentRepCount = repStats.total

                            // Phase state reset on new rep
                            if (currentRepCount != lastRepNumber) {
                                capturedPhasesThisRep.clear()
                                lastRepNumber = currentRepCount
                            }

                            // Capture Logic with Validity Check
                            if (result is PoseDetectionResult.Success && _uiState.value.isRecording) {
                                // Performance: Only attempt capture if landmarks meet a confidence threshold
                                if (workoutState.poseQualityScore > 0.4f) {
                                    capturePhaseFrameIfNeeded(
                                        result = result,
                                        repNumber = currentRepCount,
                                        formScore = workoutState.poseQualityScore,
                                        formFeedback = workoutState.formFeedback
                                    )
                                }
                            }

                            // Batch update the UI state once per frame
                            _uiState.update {
                                it.copy(
                                    currentPose = workoutState.currentPose,
                                    formFeedback = workoutState.formFeedback,
                                    poseQualityScore = workoutState.poseQualityScore,
                                    repCount = workoutState.repCount,
                                    goodReps = repStats.good,
                                    badReps = repStats.bad,
                                    reps = workoutState.reps,
                                    poseQuality = workoutState.poseQuality
                                )
                            }
                        }
                    }
                    .launchIn(viewModelScope)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPoseDetectionInitializing = false,
                        currentPose = PoseDetectionResult.Error("Detection failed: ${e.message}"),
                        formFeedback = "Camera unavailable"
                    )
                }
            }
        }
    }

    fun processFrame(bitmap: Bitmap, timestamp: Long, release: () -> Unit) {
        if (_uiState.value.isRecording) {
            updateLatestFrame(bitmap, timestamp)
        }
        poseDetector.detectPoseAsync(bitmap, timestamp, release)
    }

    /**
     * Optimized Frame Update: Reduces GC pressure by only copying frames
     * when recording and limiting copy frequency to 15fps for keyframe candidates.
     */
    fun updateLatestFrame(bitmap: Bitmap, timestamp: Long) {
        synchronized(bitmapLock) {
            // Memory Optimization: Skip copying if we already have a very recent frame (within 33ms)
            if (timestamp - latestFrameTimestamp < 33L) return

            val oldBitmap = latestFrameBitmap
            latestFrameBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            latestFrameTimestamp = timestamp
            oldBitmap?.recycle()
        }
    }

    private fun capturePhaseFrameIfNeeded(
        result: PoseDetectionResult.Success,
        repNumber: Int,
        formScore: Float,
        formFeedback: String
    ) {
        val exercise = workoutEngine.getCurrentExercise() ?: return
        val currentPhase = exercise.detectMovementPhase(result)

        // Focus only on high-value diagnostic phases
        val importantPhases = setOf(MovementPhase.BOTTOM, MovementPhase.LOCKOUT, MovementPhase.SETUP)

        if (currentPhase in importantPhases && currentPhase !in capturedPhasesThisRep) {
            val (bitmap, timestamp) = synchronized(bitmapLock) {
                val bmp = latestFrameBitmap ?: return
                Pair(bmp, latestFrameTimestamp)
            }

            val diagnostics = exercise.getFormDiagnostics(result)

            keyFrameCapture.captureFrame(
                bitmap = bitmap,
                timestamp = timestamp,
                repNumber = repNumber,
                poseData = result,
                formScore = formScore,
                formIssues = if (formScore < 0.7f) listOf(formFeedback) else emptyList(),
                movementPhase = currentPhase,
                diagnostics = diagnostics
            )
            capturedPhasesThisRep.add(currentPhase)
        }
    }

    suspend fun finishWorkout(): WorkoutReport? = withContext(dispatchers.default) {
        val session = workoutEngine.endSession() ?: return@withContext null
        val report = WorkoutAnalyzer.analyzeSession(session)
        lastWorkoutReport = report
        workoutReportHolder.setReport(report)

        withContext(dispatchers.io) {
            val repDataList = report.repAnalyses.map { repAnalysis ->
                RepDataDto(
                    repNumber = repAnalysis.repNumber,
                    quality = repAnalysis.quality,
                    isGoodForm = repAnalysis.isGoodForm,
                    durationMs = repAnalysis.tempo?.totalMs ?: 0L,
                    timestamp = System.currentTimeMillis(),
                    depth = repAnalysis.depth,
                    formIssues = repAnalysis.formIssues
                )
            }

            val keyFrames = keyFrameCapture.processAndSaveKeyFrames(session.id)

            workoutRepository.saveWorkout(WorkoutSessionEntity(
                sessionId = session.id,
                exerciseType = report.exerciseType.name,
                totalReps = report.totalReps,
                goodReps = report.goodReps,
                badReps = report.badReps,
                averageQuality = report.averageQuality,
                durationMs = report.durationMs,
                overallScore = report.overallScore,
                grade = report.grade,
                videoUri = videoUri,
                weight = weight,
                timestamp = System.currentTimeMillis(),
                userId = "local",
                repDataJson = Gson().toJson(repDataList),
                keyFramesJson = Gson().toJson(keyFrames)
            ))
            keyFrameCapture.clear()
        }

        synchronized(bitmapLock) {
            latestFrameBitmap?.recycle()
            latestFrameBitmap = null
        }
        report
    }

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true) }
        workoutEngine.startSession(_uiState.value.mode)
    }

    fun stopRecording() {
        _uiState.update { it.copy(isRecording = false) }
        stopPoseDetection()
    }

    fun setVideoUri(uri: String) { videoUri = uri }
    fun setWeight(w: Float) { weight = w }
    fun getLastWorkoutReport(): WorkoutReport? = lastWorkoutReport
    fun setExerciseType(exerciseType: ExerciseType) {
        workoutEngine.setExerciseType(exerciseType)
        _uiState.update { it.copy(exerciseType = exerciseType) }
    }

    fun stopPoseDetection() { _uiState.update { it.copy(isPoseDetectionInitializing = false) } }

    fun resetReps() {
        workoutEngine.reset()
        _uiState.update {
            it.copy(
                repCount = 0,
                goodReps = 0,
                badReps = 0,
                reps = emptyList()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        poseDetector.reset()
        synchronized(bitmapLock) { latestFrameBitmap?.recycle() }
    }
}