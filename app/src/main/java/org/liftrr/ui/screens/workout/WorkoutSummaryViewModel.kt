package org.liftrr.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.liftrr.domain.analytics.WorkoutReport
import org.liftrr.ml.WorkoutLLM
import javax.inject.Inject

/**
 * ViewModel for WorkoutSummaryScreen with AI-enhanced insights
 */
@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    private val workoutLLM: WorkoutLLM
) : ViewModel() {

    private val _aiSummary = MutableStateFlow<AIInsightState>(AIInsightState.Idle)
    val aiSummary: StateFlow<AIInsightState> = _aiSummary.asStateFlow()

    private val _aiRecommendations = MutableStateFlow<AIInsightState>(AIInsightState.Idle)
    val aiRecommendations: StateFlow<AIInsightState> = _aiRecommendations.asStateFlow()

    private val _motivationalMessage = MutableStateFlow<String?>(null)
    val motivationalMessage: StateFlow<String?> = _motivationalMessage.asStateFlow()

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    init {
        // Initialize the LLM on background thread when ViewModel is created
        viewModelScope.launch {
            _isInitializing.value = true
            try {
                // Offload LLM initialization to IO thread to prevent ANR
                withContext(Dispatchers.IO) {
                    workoutLLM.initialize()
                }
            } catch (e: Exception) {
                // LLM initialization failed, insights will show error state
                _aiSummary.value = AIInsightState.Error("AI model not available: ${e.message}")
                _aiRecommendations.value = AIInsightState.Error("AI model not available")
            } finally {
                _isInitializing.value = false
            }
        }
    }

    /**
     * Generate all AI insights for the workout report
     */
    fun generateInsights(report: WorkoutReport) {
        viewModelScope.launch {
            // Generate workout summary
            generateSummary(report)

            // Generate personalized recommendations
            generateRecommendations(report)

            // Generate motivational message based on performance
            generateMotivation(report)
        }
    }

    private suspend fun generateSummary(report: WorkoutReport) {
        _aiSummary.value = AIInsightState.Loading

        try {
            // Offload LLM inference to IO thread to prevent ANR
            val summary = withContext(Dispatchers.IO) {
                workoutLLM.generateWorkoutSummary(report)
            }

            _aiSummary.value = if (summary != null) {
                AIInsightState.Success(summary)
            } else {
                AIInsightState.Error("Could not generate summary")
            }
        } catch (e: Exception) {
            _aiSummary.value = AIInsightState.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun generateRecommendations(report: WorkoutReport) {
        _aiRecommendations.value = AIInsightState.Loading

        try {
            // Offload LLM inference to IO thread to prevent ANR
            val recommendations = withContext(Dispatchers.IO) {
                workoutLLM.generatePersonalizedRecommendations(
                    report = report,
                    userGoals = null // Could be retrieved from user profile
                )
            }

            _aiRecommendations.value = if (recommendations != null) {
                AIInsightState.Success(recommendations)
            } else {
                AIInsightState.Error("Could not generate recommendations")
            }
        } catch (e: Exception) {
            _aiRecommendations.value = AIInsightState.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun generateMotivation(report: WorkoutReport) {
        try {
            val context = when {
                report.overallScore >= 90 -> "after an excellent workout"
                report.overallScore >= 75 -> "after a good workout"
                report.overallScore >= 60 -> "after completing a challenging workout"
                else -> "to keep improving"
            }

            // Offload LLM inference to IO thread to prevent ANR
            val message = withContext(Dispatchers.IO) {
                workoutLLM.generateMotivation(context)
            }
            _motivationalMessage.value = message
        } catch (e: Exception) {
            // Motivation is optional, silently fail
            _motivationalMessage.value = null
        }
    }

    /**
     * Retry generating insights if failed
     */
    fun retryInsights(report: WorkoutReport) {
        generateInsights(report)
    }
}

/**
 * State for AI-generated insights
 */
sealed class AIInsightState {
    object Idle : AIInsightState()
    object Loading : AIInsightState()
    data class Success(val text: String) : AIInsightState()
    data class Error(val message: String) : AIInsightState()
}
