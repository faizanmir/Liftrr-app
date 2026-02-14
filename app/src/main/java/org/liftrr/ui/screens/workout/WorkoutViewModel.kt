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
    @ApplicationContext private val context: Context,
    val poseDetector: PoseDetector,
    private val workoutReportHolder: org.liftrr.domain.workout.WorkoutReportHolder,
    private val workoutRepository: WorkoutRepository,
    val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WorkoutUiState(mode = WorkoutMode.SENSOR_AND_CAMERA)
    )
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    // Business logic engine (follows Dependency Inversion Principle)
    private val workoutEngine = WorkoutEngine()

    // Key frame capture for visual feedback
    private val keyFrameCapture = KeyFrameCapture(context)

    // Store the last generated report for summary screen
    private var lastWorkoutReport: WorkoutReport? = null

    // Store video URI for the current workout
    private var videoUri: String? = null

    // Store weight for the current workout
    private var weight: Float? = null

    // Track if pose detector is already initialized
    private var isPoseDetectorInitialized = false

    // Store latest camera frame for key frame capture
    private var latestFrameBitmap: Bitmap? = null
    private var latestFrameTimestamp: Long = 0L

    // Track which phases have been captured for the current rep
    private val capturedPhasesThisRep = mutableSetOf<MovementPhase>()
    private var lastRepNumber = 0

    /**
     * Pre-initialize pose detector to reduce startup delay
     * Called early (e.g., on WorkoutPreparationScreen) to prepare for workout
     */
    fun preInitializePoseDetector() {
        if (isPoseDetectorInitialized) return

        viewModelScope.launch {
            try {
                withContext(dispatchers.io) {
                    poseDetector.initialize()
                }
                isPoseDetectorInitialized = true
                android.util.Log.d("WorkoutViewModel", "Pose detector pre-initialized successfully")
            } catch (e: Exception) {
                android.util.Log.e("WorkoutViewModel", "Failed to pre-initialize pose detector", e)
            }
        }
    }

    fun observePoseDetection() {
        viewModelScope.launch {
            try {
                if (!isPoseDetectorInitialized) {
                    _uiState.update { it.copy(isPoseDetectionInitializing = true) }

                    withContext(dispatchers.io) {
                        poseDetector.initialize()
                    }
                    isPoseDetectorInitialized = true

                    _uiState.update { it.copy(isPoseDetectionInitializing = false) }
                }

                poseDetector.poseResults
                    .onEach { result ->
                        withContext(dispatchers.default) {
                            val previousRepCount = workoutEngine.getRepStats().total
                            val workoutState = workoutEngine.processPoseResult(result)
                            val repStats = workoutEngine.getRepStats()
                            val currentRepCount = repStats.total

                            // Reset captured phases when starting a new rep
                            if (currentRepCount != lastRepNumber) {
                                capturedPhasesThisRep.clear()
                                lastRepNumber = currentRepCount
                            }

                            // Capture frames at different phases during the rep
                            if (result is PoseDetectionResult.Success && _uiState.value.isRecording && currentRepCount > 0) {
                                capturePhaseFrameIfNeeded(
                                    result = result,
                                    repNumber = currentRepCount,
                                    formScore = workoutState.poseQualityScore,
                                    formFeedback = workoutState.formFeedback
                                )
                            }

                            withContext(dispatchers.main) {
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
                    }
                    .launchIn(viewModelScope)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPoseDetectionInitializing = false,
                        currentPose = PoseDetectionResult.Error("Failed to load pose detection model: ${e.message}"),
                        formFeedback = "Camera unavailable"
                    )
                }
            }
        }
    }

    fun processFrame(bitmap: android.graphics.Bitmap, timestamp: Long, release: () -> Unit) {
        // Store latest frame for key frame capture (only if recording)
        if (_uiState.value.isRecording) {
            updateLatestFrame(bitmap, timestamp)
        }

        poseDetector.detectPoseAsync(bitmap, timestamp, release)
    }

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true) }
        // Start session tracking for analytics
        workoutEngine.startSession(_uiState.value.mode)
    }

    fun stopRecording() {
        _uiState.update { it.copy(isRecording = false) }
        stopPoseDetection()
    }

    /**
     * Set the video URI for the current workout
     */
    fun setVideoUri(uri: String) {
        android.util.Log.d("WorkoutViewModel", "Video URI set: $uri")
        videoUri = uri
    }

    /**
     * Set the weight for the current workout
     */
    fun setWeight(w: Float) {
        android.util.Log.d("WorkoutViewModel", "Weight set: $w kg")
        weight = w
    }

    /**
     * Store the latest camera frame for potential key frame capture
     * Called from UI when camera provides a new frame
     */
    fun updateLatestFrame(bitmap: Bitmap, timestamp: Long) {
        // Recycle old bitmap
        latestFrameBitmap?.recycle()
        // Make a copy since the original will be recycled by the pool
        latestFrameBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        latestFrameTimestamp = timestamp
    }

    /**
     * Capture frames at different phases of the movement
     * Detects current phase and captures if not already captured this rep
     */
    private fun capturePhaseFrameIfNeeded(
        result: PoseDetectionResult.Success,
        repNumber: Int,
        formScore: Float,
        formFeedback: String
    ) {
        val bitmap = latestFrameBitmap ?: return

        // Detect current movement phase
        val currentPhase = workoutEngine.getCurrentExercise()?.detectMovementPhase(result) ?: return

        // Define which phases we want to capture (4-5 key phases)
        val importantPhases = setOf(
            MovementPhase.SETUP,
            MovementPhase.DESCENT,
            MovementPhase.BOTTOM,
            MovementPhase.ASCENT,
            MovementPhase.LOCKOUT
        )

        // Only capture if this is an important phase and we haven't captured it yet for this rep
        if (currentPhase in importantPhases && currentPhase !in capturedPhasesThisRep) {
            // Determine form issues from feedback
            val formIssues = if (formScore < 0.7f) {
                listOf(formFeedback)
            } else {
                emptyList()
            }

            // Capture this frame for later processing
            keyFrameCapture.captureFrame(
                bitmap = bitmap,
                timestamp = latestFrameTimestamp,
                repNumber = repNumber,
                poseData = result,
                formScore = formScore,
                formIssues = formIssues,
                movementPhase = currentPhase
            )

            // Mark this phase as captured for this rep
            capturedPhasesThisRep.add(currentPhase)
        }
    }

    suspend fun finishWorkout(): WorkoutReport? = withContext(dispatchers.default) {
        android.util.Log.d("WorkoutViewModel", "finishWorkout called. videoUri: $videoUri")

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

            val repDataJson = try {
                Gson().toJson(repDataList)
            } catch (e: Exception) {
                android.util.Log.e("WorkoutViewModel", "Failed to serialize rep data", e)
                null
            }

            // Process and save key frames
            val keyFrames = keyFrameCapture.processAndSaveKeyFrames(session.id)
            val keyFramesJson = try {
                if (keyFrames.isNotEmpty()) {
                    Gson().toJson(keyFrames)
                } else null
            } catch (e: Exception) {
                android.util.Log.e("WorkoutViewModel", "Failed to serialize key frames", e)
                null
            }

            // Clear captured frames to free memory
            keyFrameCapture.clear()

            val entity = WorkoutSessionEntity(
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
                isUploaded = false,
                repDataJson = repDataJson,
                keyFramesJson = keyFramesJson
            )
            android.util.Log.d("WorkoutViewModel", "Saving workout to database with ${repDataList.size} reps, ${keyFrames.size} key frames, video URI: ${entity.videoUri}, weight: ${entity.weight}")
            workoutRepository.saveWorkout(entity)
        }

        // Clean up latest frame bitmap
        latestFrameBitmap?.recycle()
        latestFrameBitmap = null

        report
    }

    fun getLastWorkoutReport(): WorkoutReport? = lastWorkoutReport

    fun getCurrentSession(): WorkoutSession? {
        return if (workoutEngine.isSessionActive()) {
            workoutEngine.endSession()
        } else {
            null
        }
    }

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

    fun setExerciseType(exerciseType: ExerciseType) {
        workoutEngine.setExerciseType(exerciseType)
        _uiState.update { it.copy(exerciseType = exerciseType) }
    }

    /**
     * Called when stopping recording
     * Stops the pose detector to release resources
     */
    fun stopPoseDetection() {
        _uiState.update { it.copy(isPoseDetectionInitializing = false) }
    }

    override fun onCleared() {
        super.onCleared()
        poseDetector.reset()
    }
}
