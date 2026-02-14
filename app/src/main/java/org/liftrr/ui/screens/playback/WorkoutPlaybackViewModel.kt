package org.liftrr.ui.screens.playback

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.liftrr.data.models.RepDataDto
import org.liftrr.data.repository.WorkoutRepository
import org.liftrr.utils.DispatcherProvider
import org.liftrr.utils.WorkoutReportExporter
import javax.inject.Inject

@HiltViewModel
class WorkoutPlaybackViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutRepository: WorkoutRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Loading)
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun loadWorkoutSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val workout = workoutRepository.getWorkoutById(sessionId)

                if (workout != null) {
                    // Deserialize rep data if available
                    val repData = workout.repDataJson?.let { json ->
                        try {
                            val type = object : TypeToken<List<RepDataDto>>() {}.type
                            Gson().fromJson<List<RepDataDto>>(json, type)
                        } catch (e: Exception) {
                            android.util.Log.e("WorkoutPlaybackViewModel", "Failed to deserialize rep data", e)
                            null
                        }
                    }

                    _uiState.value = PlaybackUiState.Success(
                        WorkoutPlaybackData(
                            sessionId = workout.sessionId,
                            exerciseName = formatExerciseName(workout.exerciseType),
                            totalReps = workout.totalReps,
                            goodReps = workout.goodReps,
                            badReps = workout.badReps,
                            averageQuality = workout.averageQuality,
                            durationMs = workout.durationMs,
                            overallScore = workout.overallScore,
                            grade = workout.grade,
                            videoUri = workout.videoUri,
                            timestamp = workout.timestamp,
                            repData = repData
                        )
                    )
                } else {
                    _uiState.value = PlaybackUiState.Error("Workout session not found")
                }
            } catch (e: Exception) {
                _uiState.value = PlaybackUiState.Error("Failed to load workout: ${e.message}")
            }
        }
    }

    private fun formatExerciseName(exerciseType: String): String {
        return when (exerciseType) {
            "SQUAT" -> "Squat"
            "DEADLIFT" -> "Deadlift"
            "BENCH_PRESS" -> "Bench Press"
            else -> exerciseType.replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Share workout summary as text
     */
    fun shareAsText(workoutData: WorkoutPlaybackData) {
        viewModelScope.launch(dispatchers.io) {
            try {
                _isExporting.value = true
                android.util.Log.d("WorkoutPlaybackVM", "Sharing workout as text for session ${workoutData.sessionId}")
                val text = WorkoutReportExporter.exportSimpleSummaryAsText(
                    exerciseName = workoutData.exerciseName,
                    totalReps = workoutData.totalReps,
                    goodReps = workoutData.goodReps,
                    badReps = workoutData.badReps,
                    overallScore = workoutData.overallScore,
                    grade = workoutData.grade,
                    durationMs = workoutData.durationMs
                )
                android.util.Log.d("WorkoutPlaybackVM", "Text summary created, length: ${text.length}")

                withContext(dispatchers.main) {
                    WorkoutReportExporter.shareAsText(context, text)
                    android.util.Log.d("WorkoutPlaybackVM", "Share intent launched")
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkoutPlaybackVM", "Failed to share workout as text", e)
                e.printStackTrace()
            } finally {
                _isExporting.value = false
            }
        }
    }

    /**
     * Share workout as PDF with full details and key frames
     */
    fun shareAsPdf(sessionId: String) {
        viewModelScope.launch(dispatchers.io) {
            try {
                _isExporting.value = true
                android.util.Log.d("WorkoutPlaybackVM", "Sharing workout as PDF for session $sessionId")

                // Load the full workout data from repository
                val workout = workoutRepository.getWorkoutById(sessionId)

                if (workout != null) {
                    // Reconstruct a minimal WorkoutReport for PDF export
                    val report = org.liftrr.domain.analytics.WorkoutReport(
                        sessionId = workout.sessionId,
                        exerciseType = when (workout.exerciseType) {
                            "SQUAT" -> org.liftrr.ml.ExerciseType.SQUAT
                            "DEADLIFT" -> org.liftrr.ml.ExerciseType.DEADLIFT
                            "BENCH_PRESS" -> org.liftrr.ml.ExerciseType.BENCH_PRESS
                            else -> org.liftrr.ml.ExerciseType.SQUAT
                        },
                        totalReps = workout.totalReps,
                        goodReps = workout.goodReps,
                        badReps = workout.badReps,
                        averageQuality = workout.averageQuality,
                        durationMs = workout.durationMs,
                        rangeOfMotion = org.liftrr.domain.analytics.RangeOfMotionAnalysis(
                            averageDepth = 0f,
                            minDepth = 0f,
                            maxDepth = 0f,
                            consistency = 0f
                        ),
                        tempo = org.liftrr.domain.analytics.TempoAnalysis(
                            averageRepDurationMs = 0L,
                            averageRestBetweenRepsMs = 0L,
                            tempoConsistency = 0f
                        ),
                        symmetry = org.liftrr.domain.analytics.SymmetryAnalysis(
                            overallSymmetry = 0f,
                            leftRightAngleDifference = 0f,
                            issues = emptyList()
                        ),
                        formConsistency = org.liftrr.domain.analytics.FormConsistencyAnalysis(
                            consistencyScore = 0f,
                            qualityTrend = "N/A"
                        ),
                        repAnalyses = emptyList(),
                        recommendations = emptyList(),
                        exerciseSpecificMetrics = null
                    )

                    val keyFramesJson = workout.keyFramesJson
                    android.util.Log.d("WorkoutPlaybackVM", "Loaded workout, keyFrames present: ${keyFramesJson != null}")

                    val pdfFile = WorkoutReportExporter.exportAsPdf(context, report, keyFramesJson)
                    android.util.Log.d("WorkoutPlaybackVM", "PDF created at: ${pdfFile.absolutePath}, exists: ${pdfFile.exists()}")

                    withContext(dispatchers.main) {
                        WorkoutReportExporter.shareReport(context, pdfFile, "application/pdf")
                        android.util.Log.d("WorkoutPlaybackVM", "Share PDF intent launched")
                    }
                } else {
                    android.util.Log.e("WorkoutPlaybackVM", "Workout not found for session $sessionId")
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkoutPlaybackVM", "Failed to share workout as PDF", e)
                e.printStackTrace()
            } finally {
                _isExporting.value = false
            }
        }
    }
}

sealed class PlaybackUiState {
    data object Loading : PlaybackUiState()
    data class Success(val workoutData: WorkoutPlaybackData) : PlaybackUiState()
    data class Error(val message: String) : PlaybackUiState()
}

data class WorkoutPlaybackData(
    val sessionId: String,
    val exerciseName: String,
    val totalReps: Int,
    val goodReps: Int,
    val badReps: Int,
    val averageQuality: Float,
    val durationMs: Long,
    val overallScore: Float,
    val grade: String,
    val videoUri: String?,
    val timestamp: Long,
    val repData: List<RepDataDto>? = null
)
