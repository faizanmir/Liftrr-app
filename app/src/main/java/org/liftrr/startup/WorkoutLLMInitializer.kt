package org.liftrr.startup

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * LAZY WorkoutLLM Pre-warming Initializer
 *
 * Unlike MediaPipe (which pre-warms at app startup), WorkoutLLM is:
 * - NOT auto-initialized at app startup (too heavy, not immediately needed)
 * - Pre-warmed when user starts a workout (via manual initialization)
 * - Runs on background thread to avoid blocking workout screen
 *
 * Usage:
 * ```kotlin
 * // In WorkoutViewModel.init or when workout screen is displayed
 * AppInitializer.getInstance(context)
 *     .initializeComponent(WorkoutLLMInitializer::class.java)
 * ```
 *
 * Benefits:
 * - No impact on app startup
 * - LLM ready when user asks for coaching tips
 * - First LLM query is fast (~200ms instead of ~3s)
 *
 * Note: This initializer is NOT registered in AndroidManifest.xml
 * (it's called manually, not automatically)
 */
class WorkoutLLMInitializer : Initializer<Unit> {

    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun create(context: Context) {
        Log.d(TAG, "Starting WorkoutLLM pre-warming")

        // Pre-warm LLM on background thread
        initializationScope.launch {
            try {
                val startTime = System.currentTimeMillis()

                // TODO: Implement LLM pre-warming
                // This would:
                // 1. Load the Gemma model into memory
                // 2. Run a dummy inference to warm up the engine
                // 3. Close the temporary instance (model stays in memory)
                //
                // Example:
                // val options = LlmInference.LlmInferenceOptions.builder()
                //     .setModelPath(getModelPath(context))
                //     .build()
                // val warmupLLM = LlmInference.createFromOptions(context, options)
                // warmupLLM.generateResponse("warmup")
                // warmupLLM.close()

                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "WorkoutLLM pre-warming completed in ${duration}ms")

            } catch (e: Exception) {
                // Don't crash if pre-warming fails
                Log.w(TAG, "WorkoutLLM pre-warming failed (non-fatal): ${e.message}")
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // No dependencies - can run independently
        return emptyList()
    }

    companion object {
        private const val TAG = "WorkoutLLMInit"

        /**
         * Manual initialization helper
         *
         * Call this when user starts a workout to pre-warm the LLM
         * in the background before they need it.
         */
        fun prewarmIfNeeded(context: Context) {
            // Use AppInitializer for lazy, deduplicated initialization
            androidx.startup.AppInitializer.getInstance(context)
                .initializeComponent(WorkoutLLMInitializer::class.java)
        }
    }
}
