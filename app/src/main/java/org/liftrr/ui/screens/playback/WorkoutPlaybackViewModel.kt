package org.liftrr.ui.screens.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.liftrr.data.models.RepDataDto
import org.liftrr.data.repository.WorkoutRepository
import javax.inject.Inject

@HiltViewModel
class WorkoutPlaybackViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Loading)
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

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
