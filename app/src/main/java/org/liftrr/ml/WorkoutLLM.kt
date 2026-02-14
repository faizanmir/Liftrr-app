package org.liftrr.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.liftrr.domain.analytics.WorkoutReport
import org.liftrr.domain.workout.RepData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device LLM for intelligent workout coaching and analysis
 *
 * Uses MediaPipe LLM Inference (Gemma models) to provide:
 * - Real-time form coaching
 * - Natural language workout summaries
 * - Personalized recommendations
 * - Exercise Q&A
 * - Multimodal analysis (pose + images)
 *
 * Features:
 * - Completely on-device (no cloud required)
 * - Privacy-preserving
 * - Low latency responses
 * - Context-aware conversations
 *
 * Usage:
 * ```kotlin
 * // Initialize
 * workoutLLM.initialize()
 *
 * // Ask questions
 * val response = workoutLLM.generateCoachingTip(
 *     exercise = ExerciseType.SQUAT,
 *     formIssue = "knees caving in"
 * )
 *
 * // Generate workout summary
 * val summary = workoutLLM.generateWorkoutSummary(report)
 *
 * // Multimodal analysis
 * val analysis = workoutLLM.analyzeFormWithImage(
 *     bitmap = screenshot,
 *     context = "User is performing a squat"
 * )
 * ```
 */
@Singleton
class WorkoutLLM @Inject constructor(
    @ApplicationContext private val context: Context
) {

    @Volatile
    private var llmInference: LlmInference? = null

    @Volatile
    private var isInitialized = false

    // Mutex to serialize LLM access (MediaPipe LLM is not thread-safe)
    private val llmMutex = Mutex()

    private val _responses = Channel<LLMResponse>(Channel.CONFLATED)
    val responses: Flow<LLMResponse> = _responses.receiveAsFlow()

    companion object {
        private const val TAG = "WorkoutLLM"

        // Generation parameters for MediaPipe LLM
        private const val MAX_TOKENS = 512
        private const val RESPONSE_LOG_PROBS = false
        private const val RANDOM_SEED = 0

        // System prompts
        private const val SYSTEM_PROMPT = """You are an expert fitness coach and personal trainer.
Your role is to provide helpful, accurate, and encouraging workout guidance.
Keep responses concise (2-3 sentences) unless detailed explanation is requested.
Focus on safety, proper form, and progressive improvement."""

        /**
         * Get model path based on build type
         *
         * Development: Uses pushed model from /data/local/tmp/llm/
         * Production: Uses bundled model from assets or downloaded to internal storage
         */
        private fun getModelPath(context: Context): String {
            // Try development paths first (pushed via adb)
            // MediaPipe LLM uses .task format
            val devPaths = listOf(
                "/data/local/tmp/llm/model.task",
                "/data/local/tmp/llm/model.bin"
            )

            for (devPath in devPaths) {
                if (java.io.File(devPath).exists()) {
                    android.util.Log.d(TAG, "Using development model: $devPath")
                    return devPath
                }
            }

            // Try bundled assets model (.task is MediaPipe format)
            val assetModels = listOf(
                "llm/gemma-3-1b.task",
                "llm/gemma-2b.task",
                "llm/gemma-1b.task",
                "llm/model.task",
                "llm/gemma-2b.bin",
                "llm/gemma-1b.bin",
                "llm/model.bin"
            )

            for (assetPath in assetModels) {
                try {
                    context.assets.open(assetPath).use {
                        // Model exists in assets, copy to cache for MediaPipe
                        val cacheFile = java.io.File(context.cacheDir, "llm_model.bin")
                        if (!cacheFile.exists()) {
                            android.util.Log.d(TAG, "Copying model from assets: $assetPath")
                            cacheFile.outputStream().use { output ->
                                it.copyTo(output)
                            }
                        }
                        android.util.Log.d(TAG, "Using bundled model: $assetPath")
                        return cacheFile.absolutePath
                    }
                } catch (e: Exception) {
                    // Asset doesn't exist, try next
                    continue
                }
            }

            // Try downloaded model in internal storage
            val downloadedPaths = listOf(
                java.io.File(context.filesDir, "llm_model.task"),
                java.io.File(context.filesDir, "llm_model.bin")
            )

            for (downloadedPath in downloadedPaths) {
                if (downloadedPath.exists()) {
                    android.util.Log.d(TAG, "Using downloaded model: ${downloadedPath.absolutePath}")
                    return downloadedPath.absolutePath
                }
            }

            throw IllegalStateException(
                "No LLM model found. Please:\n" +
                "1. For development: Copy .task file to app/src/main/assets/llm/ then run './gradlew pushLLMModel'\n" +
                "2. For production: Bundle model in assets or implement runtime download\n" +
                "See LLM_MODEL_SETUP.md for details."
            )
        }
    }

    /**
     * Initialize the LLM
     * Must be called before using any generation methods
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "LLM already initialized")
            return
        }

        try {
            val modelPath = getModelPath(context)
            Log.d(TAG, "Initializing LLM with model: $modelPath")

            // Create options with minimal configuration
            // MediaPipe GenAI 0.10.32 API
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(MAX_TOKENS)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isInitialized = true
            Log.d(TAG, "LLM initialized successfully with model at: $modelPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LLM", e)
            isInitialized = false
            throw IllegalStateException("Failed to initialize WorkoutLLM: ${e.message}", e)
        }
    }

    /**
     * Generate coaching tip for specific form issue
     */
    suspend fun generateCoachingTip(
        exercise: ExerciseType,
        formIssue: String
    ): String? {
        val prompt = """
            Exercise: ${exercise.name.replace("_", " ")}
            Form Issue: $formIssue

            Provide a brief, actionable coaching tip to fix this form issue.
        """.trimIndent()

        return generateResponse(prompt)
    }

    /**
     * Generate natural language workout summary
     */
    suspend fun generateWorkoutSummary(report: WorkoutReport): String? {
        // Build exercise-specific metrics summary
        val exerciseSpecificInfo = when (val metrics = report.exerciseSpecificMetrics) {
            is org.liftrr.domain.analytics.ExerciseSpecificMetrics.SquatMetrics -> """
                Squat-Specific:
                - Depth: ${metrics.averageDepth.toInt()}° (${if (metrics.averageDepth < 90) "Excellent depth" else if (metrics.averageDepth < 110) "Good depth" else "Work on depth"})
                - Knee Tracking: ${metrics.kneeTracking.kneeAlignment.toInt()}% (${if (metrics.kneeTracking.kneeCavePercentage > 30) "Knee cave detected in ${metrics.kneeTracking.kneeCavePercentage.toInt()}% of reps" else "Good tracking"})
                - Hip Mobility: ${metrics.hipMobility.toInt()}%
                - Torso Angle: ${metrics.torsoAngle.averageForwardLean.toInt()}° forward lean
            """.trimIndent()
            is org.liftrr.domain.analytics.ExerciseSpecificMetrics.DeadliftMetrics -> """
                Deadlift-Specific:
                - Hip Hinge Quality: ${metrics.hipHingeQuality.toInt()}%
                - Back Straightness: ${metrics.backStraightness.spineNeutral.toInt()}% (${if (metrics.backStraightness.lowerBackRounding > 20) "Lower back rounding detected" else "Good neutral spine"})
                - Lockout: ${metrics.lockoutCompletion.toInt()}% completion
                - Starting Position: ${metrics.startingPosition.setup.toInt()}%
            """.trimIndent()
            is org.liftrr.domain.analytics.ExerciseSpecificMetrics.BenchPressMetrics -> """
                Bench Press-Specific:
                - Elbow Angle: ${metrics.elbowAngle.bottomAngle.toInt()}° at bottom
                - Elbow Tucking: ${metrics.elbowAngle.tucking.toInt()}%
                - Shoulder Stability: ${metrics.shoulderPosition.stability.toInt()}%
                - Touch Point Consistency: ${metrics.touchPointConsistency.toInt()}%
            """.trimIndent()
            null -> ""
        }

        val prompt = """
            Generate a brief, encouraging workout summary:

            Exercise: ${report.exerciseType.name.replace("_", " ")}
            Total Reps: ${report.totalReps} (${report.goodReps} good, ${report.badReps} bad)
            Overall Score: ${report.overallScore.toInt()}% (Grade: ${report.grade})
            Duration: ${formatDuration(report.durationMs)}

            Key Metrics:
            - Depth Consistency: ${report.rangeOfMotion.consistency.toInt()}%
            - Symmetry: ${report.symmetry.overallSymmetry.toInt()}%
            - Form Trend: ${report.formConsistency.qualityTrend}

            $exerciseSpecificInfo

            Write 2-3 sentences highlighting strengths and one area for improvement, focusing on the exercise-specific metrics.
        """.trimIndent()

        return generateResponse(prompt)
    }

    /**
     * Generate personalized recommendations based on workout data
     */
    suspend fun generatePersonalizedRecommendations(
        report: WorkoutReport,
        userGoals: String? = null
    ): String? {
        // Build exercise-specific issues
        val exerciseIssues = when (val metrics = report.exerciseSpecificMetrics) {
            is org.liftrr.domain.analytics.ExerciseSpecificMetrics.SquatMetrics -> buildString {
                if (metrics.kneeTracking.kneeCavePercentage > 30) {
                    append("- Knees caving inward (${metrics.kneeTracking.kneeCavePercentage.toInt()}%)\n")
                }
                if (metrics.averageDepth > 110) {
                    append("- Limited depth (${metrics.averageDepth.toInt()}°)\n")
                }
                if (metrics.torsoAngle.excessiveLean) {
                    append("- Excessive forward lean\n")
                }
                if (metrics.hipMobility < 70) {
                    append("- Limited hip mobility\n")
                }
            }
            is org.liftrr.domain.analytics.ExerciseSpecificMetrics.DeadliftMetrics -> buildString {
                if (metrics.hipHingeQuality < 70) {
                    append("- Poor hip hinge pattern\n")
                }
                if (metrics.backStraightness.lowerBackRounding > 20) {
                    append("- Lower back rounding\n")
                }
                if (metrics.lockoutCompletion < 90) {
                    append("- Incomplete lockout\n")
                }
            }
            is org.liftrr.domain.analytics.ExerciseSpecificMetrics.BenchPressMetrics -> buildString {
                if (metrics.elbowAngle.bottomAngle > 100) {
                    append("- Not lowering deep enough\n")
                }
                if (metrics.elbowAngle.tucking < 70) {
                    append("- Poor elbow tucking\n")
                }
                if (metrics.shoulderPosition.stability < 75) {
                    append("- Unstable shoulders\n")
                }
            }
            null -> ""
        }

        val prompt = """
            Based on this workout data, provide 2-3 specific recommendations:

            Exercise: ${report.exerciseType.name.replace("_", " ")}
            Performance: ${report.goodReps}/${report.totalReps} reps with good form
            Depth Consistency: ${report.rangeOfMotion.consistency.toInt()}%
            Symmetry: ${report.symmetry.overallSymmetry.toInt()}%
            ${if (userGoals != null) "User Goal: $userGoals" else ""}

            Exercise-Specific Issues Detected:
            $exerciseIssues

            Provide actionable next steps for improvement, focusing on the specific issues detected.
        """.trimIndent()

        return generateResponse(prompt)
    }

    /**
     * Answer exercise-related questions
     */
    suspend fun answerExerciseQuestion(
        exercise: ExerciseType,
        question: String
    ): String? {
        val prompt = """
            Exercise: ${exercise.name.replace("_", " ")}
            Question: $question

            Provide a clear, concise answer focusing on proper form and safety.
        """.trimIndent()

        return generateResponse(prompt)
    }

    /**
     * Generate rep-specific feedback
     */
    suspend fun generateRepFeedback(
        exercise: ExerciseType,
        rep: RepData,
        formIssues: List<String>
    ): String? {
        val prompt = """
            Rep #${rep.repNumber} - ${exercise.name.replace("_", " ")}
            Form Quality: ${if (rep.isGoodForm) "Good" else "Needs Improvement"}
            Quality Score: ${(rep.poseQuality * 100).toInt()}%
            ${if (formIssues.isNotEmpty()) "Issues: ${formIssues.joinToString(", ")}" else ""}

            Provide brief feedback for this specific rep.
        """.trimIndent()

        return generateResponse(prompt)
    }

    /**
     * Analyze form using multimodal input (image + text)
     */
    suspend fun analyzeFormWithImage(
        bitmap: Bitmap,
        exercise: ExerciseType,
        context: String
    ): String? {
        // TODO: Multimodal analysis requires different MediaPipe API
        // Commenting out until we have the right API version
        Log.w(TAG, "Multimodal analysis not yet implemented for this MediaPipe version")
        return "Multimodal analysis coming soon"

        /* val inference = llmInference ?: return null

        try {
            // createSession() may not be available in all versions
            // val session = inference.createSession()

            val prompt = """
                Analyze this ${exercise.name.replace("_", " ")} form:
                Context: $context

                Provide specific form feedback based on what you see.
            """.trimIndent()

            // session.addQueryChunk(prompt)

            // Convert bitmap to MediaPipe Image
            // val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
            // session.addImage(mpImage)

            // val result = session.generateResponse()
            // return result
        } catch (e: Exception) {
            Log.e(TAG, "Error in multimodal analysis", e)
            return null
        } */
    }

    /**
     * Generate motivational message
     */
    suspend fun generateMotivation(
        context: String = "starting workout"
    ): String? {
        val prompt = """
            Generate a brief, energizing motivational message for someone $context.
            Keep it to 1-2 sentences.
        """.trimIndent()

        return generateResponse(prompt)
    }

    /**
     * Explain exercise technique
     */
    suspend fun explainTechnique(exercise: ExerciseType): String? {
        val prompt = """
            Explain the proper technique for ${exercise.name.replace("_", " ")} in 3-4 key points.
            Focus on the most important form cues.
        """.trimIndent()

        return generateResponse(prompt)
    }

    /**
     * Compare two workouts
     */
    suspend fun compareWorkouts(
        previousReport: WorkoutReport,
        currentReport: WorkoutReport
    ): String? {
        val prompt = """
            Compare these two ${currentReport.exerciseType.name.replace("_", " ")} workouts:

            Previous: ${previousReport.totalReps} reps, ${previousReport.overallScore.toInt()}% score
            Current: ${currentReport.totalReps} reps, ${currentReport.overallScore.toInt()}% score

            Previous Consistency: ${previousReport.rangeOfMotion.consistency.toInt()}%
            Current Consistency: ${currentReport.rangeOfMotion.consistency.toInt()}%

            Highlight improvements or regressions in 2-3 sentences.
        """.trimIndent()

        return generateResponse(prompt)
    }

    /**
     * Generate response synchronously with mutex lock to prevent concurrent access
     */
    private suspend fun generateResponse(prompt: String): String? = llmMutex.withLock {
        val inference = llmInference ?: run {
            Log.e(TAG, "LLM not initialized")
            return null
        }

        try {
            val fullPrompt = "$SYSTEM_PROMPT\n\n$prompt"
            val result = inference.generateResponse(fullPrompt)
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            return null
        }
    }

    /**
     * Generate response asynchronously with streaming
     */
    fun generateResponseAsync(prompt: String) {
        val inference = llmInference ?: run {
            Log.e(TAG, "LLM not initialized")
            _responses.trySend(LLMResponse.Error("LLM not initialized"))
            return
        }

        try {
            val fullPrompt = "$SYSTEM_PROMPT\n\n$prompt"
            inference.generateResponseAsync(fullPrompt)
            // Results arrive via ResultListener in handleStreamingResponse
        } catch (e: Exception) {
            Log.e(TAG, "Error generating async response", e)
            _responses.trySend(LLMResponse.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Handle streaming response from LLM
     */
    private fun handleStreamingResponse(partialResult: String, done: Boolean) {
        if (done) {
            _responses.trySend(LLMResponse.Complete(partialResult))
        } else {
            _responses.trySend(LLMResponse.Partial(partialResult))
        }
    }

    /**
     * Stop and release LLM resources
     * Note: This is a blocking call - use suspend version if calling from coroutine
     */
    fun stop() {
        try {
            // Wait for any ongoing generation to complete before closing
            // Use runBlocking since this is called from non-suspend contexts
            kotlinx.coroutines.runBlocking {
                llmMutex.withLock {
                    llmInference?.close()
                    llmInference = null
                    isInitialized = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping LLM", e)
        }
    }

    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            "${minutes}m ${remainingSeconds}s"
        } else {
            "${remainingSeconds}s"
        }
    }
}

/**
 * LLM response types
 */
sealed class LLMResponse {
    data class Partial(val text: String) : LLMResponse()
    data class Complete(val text: String) : LLMResponse()
    data class Error(val message: String) : LLMResponse()
}
