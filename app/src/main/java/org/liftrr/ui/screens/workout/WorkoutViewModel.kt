package org.liftrr.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import org.liftrr.domain.workout.RepData
import org.liftrr.domain.workout.WorkoutEngine
import org.liftrr.ml.ExerciseType
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseDetector
import org.liftrr.ml.PoseQuality
import org.liftrr.ui.screens.session.WorkoutMode
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
    val poseDetector: PoseDetector,
    private val workoutReportHolder: org.liftrr.domain.workout.WorkoutReportHolder,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WorkoutUiState(mode = WorkoutMode.SENSOR_AND_CAMERA)
    )
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    // Business logic engine (follows Dependency Inversion Principle)
    private val workoutEngine = WorkoutEngine()

    // Store the last generated report for summary screen
    private var lastWorkoutReport: WorkoutReport? = null

    // Store video URI for the current workout
    private var videoUri: String? = null

    // Store weight for the current workout
    private var weight: Float? = null

    // Track if pose detector is already initialized
    private var isPoseDetectorInitialized = false

    /**
     * Pre-initialize pose detector to reduce startup delay
     * Called early (e.g., on WorkoutPreparationScreen) to prepare for workout
     */
    fun preInitializePoseDetector() {
        if (isPoseDetectorInitialized) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
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

                    withContext(Dispatchers.IO) {
                        poseDetector.initialize()
                    }
                    isPoseDetectorInitialized = true

                    _uiState.update { it.copy(isPoseDetectionInitializing = false) }
                }

                poseDetector.poseResults
                    .onEach { result ->
                        withContext(Dispatchers.Default) {
                            val workoutState = workoutEngine.processPoseResult(result)
                            val repStats = workoutEngine.getRepStats()

                            withContext(Dispatchers.Main) {
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

    suspend fun finishWorkout(): WorkoutReport? = withContext(Dispatchers.Default) {
        android.util.Log.d("WorkoutViewModel", "finishWorkout called. videoUri: $videoUri")

        val session = workoutEngine.endSession() ?: return@withContext null
        val report = WorkoutAnalyzer.analyzeSession(session)

        lastWorkoutReport = report
        workoutReportHolder.setReport(report)

        withContext(Dispatchers.IO) {
            val repDataList = report.repAnalyses.map { repAnalysis ->
                RepDataDto(
                    repNumber = repAnalysis.repNumber,
                    quality = repAnalysis.quality,
                    isGoodForm = repAnalysis.isGoodForm,
                    durationMs = repAnalysis.tempo?.totalMs ?: 0L,
                    timestamp = System.currentTimeMillis()
                )
            }

            val repDataJson = try {
                Gson().toJson(repDataList)
            } catch (e: Exception) {
                android.util.Log.e("WorkoutViewModel", "Failed to serialize rep data", e)
                null
            }

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
                repDataJson = repDataJson
            )
            android.util.Log.d("WorkoutViewModel", "Saving workout to database with ${repDataList.size} reps, video URI: ${entity.videoUri}, weight: ${entity.weight}")
            workoutRepository.saveWorkout(entity)
        }

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
        poseDetector.stop()
        _uiState.update { it.copy(isPoseDetectionInitializing = false) }
    }

    override fun onCleared() {
        super.onCleared()
        poseDetector.reset()
    }
}
