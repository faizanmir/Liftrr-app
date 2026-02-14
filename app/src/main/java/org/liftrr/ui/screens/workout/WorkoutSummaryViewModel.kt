package org.liftrr.ui.screens.workout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.liftrr.data.repository.WorkoutRepository
import org.liftrr.domain.analytics.WorkoutReport
import org.liftrr.ml.WorkoutLLM
import org.liftrr.utils.DispatcherProvider
import org.liftrr.utils.WorkoutReportExporter
import javax.inject.Inject

/**
 * ViewModel for WorkoutSummaryScreen with AI-enhanced insights
 */
@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutLLM: WorkoutLLM,
    private val workoutRepository: WorkoutRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _aiSummary = MutableStateFlow<AIInsightState>(AIInsightState.Idle)
    val aiSummary: StateFlow<AIInsightState> = _aiSummary.asStateFlow()

    private val _aiRecommendations = MutableStateFlow<AIInsightState>(AIInsightState.Idle)
    val aiRecommendations: StateFlow<AIInsightState> = _aiRecommendations.asStateFlow()

    private val _motivationalMessage = MutableStateFlow<String?>(null)
    val motivationalMessage: StateFlow<String?> = _motivationalMessage.asStateFlow()

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private var isLLMInitialized = false

    private suspend fun ensureLLMInitialized() {
        if (isLLMInitialized) return

        _isInitializing.value = true
        try {
            // Offload LLM initialization to IO thread to prevent ANR
            withContext(dispatchers.io) {
                workoutLLM.initialize()
            }
            isLLMInitialized = true
        } catch (e: Exception) {
            // LLM initialization failed, insights will show error state
            _aiSummary.value = AIInsightState.Error("AI model not available: ${e.message}")
            _aiRecommendations.value = AIInsightState.Error("AI model not available")
        } finally {
            _isInitializing.value = false
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
        _aiSummary.value = AIInsightState.Loading

        try {
            // Offload LLM inference to IO thread to prevent ANR
            val summary = withContext(dispatchers.io) {
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
            val recommendations = withContext(dispatchers.io) {
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
            val message = withContext(dispatchers.io) {
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

    /**
     * Export and share workout report as PDF with key frames
     */
    fun shareAsPdf(report: WorkoutReport) {
        viewModelScope.launch(dispatchers.io) {
            try {
                _isExporting.value = true
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
                _isExporting.value = false
            }
        }
    }

    /**
     * Share workout report as plain text
     */
    fun shareAsText(report: WorkoutReport) {
        viewModelScope.launch(dispatchers.io) {
            try {
                _isExporting.value = true
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
                _isExporting.value = false
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
