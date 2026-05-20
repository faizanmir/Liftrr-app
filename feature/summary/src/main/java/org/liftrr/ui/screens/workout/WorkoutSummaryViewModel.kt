package org.liftrr.ui.screens.workout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.liftrr.domain.workout.WorkoutRepository
import org.liftrr.domain.analytics.WorkoutReport
import org.liftrr.ml.WorkoutLLM
import org.liftrr.utils.DispatcherProvider
import org.liftrr.utils.WorkoutReportExporter
import javax.inject.Inject

data class WorkoutSummaryUiState(
    val aiSummary: AIInsightState = AIInsightState.Idle,
    val aiRecommendations: AIInsightState = AIInsightState.Idle,
    val motivationalMessage: String? = null,
    val isInitializing: Boolean = false,
    val isExporting: Boolean = false
)

/**
 * ViewModel for WorkoutSummaryScreen with AI-enhanced insights
 */
@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val workoutLLM: WorkoutLLM,
    private val workoutRepository: WorkoutRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState.asStateFlow()

    private var isLLMInitialized = false

    private suspend fun ensureLLMInitialized() {
        if (isLLMInitialized) return

        _uiState.update { it.copy(isInitializing = true) }
        try {
            withContext(dispatchers.io) { workoutLLM.initialize() }
            isLLMInitialized = true
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    aiSummary = AIInsightState.Error("AI model not available: ${e.message}"),
                    aiRecommendations = AIInsightState.Error("AI model not available")
                )
            }
        } finally {
            _uiState.update { it.copy(isInitializing = false) }
        }
    }

    /**
     * Generate all AI insights for the workout report
     */
    fun generateInsights(report: WorkoutReport) {
        viewModelScope.launch {
            // Ensure LLM is initialized before generating insights
            ensureLLMInitialized()

            // Generate workout summary
            generateSummary(report)

            // Generate personalized recommendations
            generateRecommendations(report)

            // Generate motivational message based on performance
            generateMotivation(report)
        }
    }

    private suspend fun generateSummary(report: WorkoutReport) {
        _uiState.update { it.copy(aiSummary = AIInsightState.Loading) }
        try {
            val summary = withContext(dispatchers.io) { workoutLLM.generateWorkoutSummary(report) }
            _uiState.update {
                it.copy(
                    aiSummary = if (summary != null) AIInsightState.Success(summary)
                    else AIInsightState.Error("Could not generate summary")
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(aiSummary = AIInsightState.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun generateRecommendations(report: WorkoutReport) {
        _uiState.update { it.copy(aiRecommendations = AIInsightState.Loading) }
        try {
            val recommendations = withContext(dispatchers.io) {
                workoutLLM.generatePersonalizedRecommendations(report = report, userGoals = null)
            }
            _uiState.update {
                it.copy(
                    aiRecommendations = if (recommendations != null) AIInsightState.Success(recommendations)
                    else AIInsightState.Error("Could not generate recommendations")
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(aiRecommendations = AIInsightState.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun generateMotivation(report: WorkoutReport) {
        try {
            val ctx = when {
                report.overallScore >= 90 -> "after an excellent workout"
                report.overallScore >= 75 -> "after a good workout"
                report.overallScore >= 60 -> "after completing a challenging workout"
                else -> "to keep improving"
            }
            val message = withContext(dispatchers.io) { workoutLLM.generateMotivation(ctx) }
            _uiState.update { it.copy(motivationalMessage = message) }
        } catch (e: Exception) {
            _uiState.update { it.copy(motivationalMessage = null) }
        }
    }

    /**
     * Retry generating insights if failed
     */
    fun retryInsights(report: WorkoutReport) {
        generateInsights(report)
    }

    /**
     * Export and share workout report as PDF with key frames
     */
    fun shareAsPdf(report: WorkoutReport) {
        viewModelScope.launch(dispatchers.io) {
            try {
                _uiState.update { it.copy(isExporting = true) }
                android.util.Log.d("WorkoutSummaryVM", "Starting PDF export for session ${report.sessionId}")

                // Load key frames from database
                val workout = workoutRepository.getWorkoutById(report.sessionId)
                val keyFramesJson = workout?.keyFramesJson
                android.util.Log.d("WorkoutSummaryVM", "Loaded workout, keyFrames present: ${keyFramesJson != null}")

                val pdfFile = WorkoutReportExporter.exportAsPdf(context, report, keyFramesJson)
                android.util.Log.d("WorkoutSummaryVM", "PDF created at: ${pdfFile.absolutePath}, exists: ${pdfFile.exists()}")

                withContext(dispatchers.main) {
                    WorkoutReportExporter.shareReport(context, pdfFile, "application/pdf")
                    android.util.Log.d("WorkoutSummaryVM", "Share intent launched")
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkoutSummaryVM", "Failed to share PDF", e)
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    /**
     * Share workout report as plain text
     */
    fun shareAsText(report: WorkoutReport) {
        viewModelScope.launch(dispatchers.io) {
            try {
                _uiState.update { it.copy(isExporting = true) }
                android.util.Log.d("WorkoutSummaryVM", "Exporting text report for session ${report.sessionId}")
                val text = WorkoutReportExporter.exportAsText(report)
                android.util.Log.d("WorkoutSummaryVM", "Text report created, length: ${text.length}")

                withContext(dispatchers.main) {
                    WorkoutReportExporter.shareAsText(context, text)
                    android.util.Log.d("WorkoutSummaryVM", "Share text intent launched")
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkoutSummaryVM", "Failed to share text", e)
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
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
