package org.liftrr.domain.workout

import org.liftrr.domain.analytics.WorkoutSession
import org.liftrr.domain.analytics.WorkoutSessionBuilder
import org.liftrr.ml.ExerciseType
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseQuality
import org.liftrr.ml.PoseQualityAnalyzer
import org.liftrr.ui.screens.session.WorkoutMode

class WorkoutEngine {

    private var currentExercise: Exercise? = null
    private var currentExerciseType: ExerciseType = ExerciseType.SQUAT
    private val reps = mutableListOf<RepData>()
    private var repCount = 0

    // Session tracking for analytics
    private var sessionBuilder: WorkoutSessionBuilder? = null
    private var isSessionActive = false

    companion object {
        private const val MIN_POSE_QUALITY = 0.5f
    }

    /**
     * Set the exercise type for the current workout
     */
    fun setExerciseType(exerciseType: ExerciseType) {
        if (currentExerciseType != exerciseType) {
            currentExerciseType = exerciseType
            currentExercise = ExerciseFactory.create(exerciseType)
            reset()
        }
    }

    /**
     * Start a new workout session for analytics
     */
    fun startSession(workoutMode: WorkoutMode) {
        sessionBuilder = WorkoutSessionBuilder(
            exerciseType = currentExerciseType,
            workoutMode = workoutMode
        )
        isSessionActive = true
    }

    /**
     * End the current workout session and return completed session
     */
    fun endSession(): WorkoutSession? {
        isSessionActive = false
        return sessionBuilder?.build(completed = true)
    }

    /**
     * Check if a session is currently active
     */
    fun isSessionActive(): Boolean = isSessionActive

    /**
     * Process a pose detection result and return workout state
     */
    fun processPoseResult(result: PoseDetectionResult): WorkoutState {
        return when (result) {
            is PoseDetectionResult.Success -> processSuccessfulPose(result)
            is PoseDetectionResult.NoPoseDetected -> WorkoutState(
                formFeedback = "Position yourself in frame",
                poseQualityScore = 0f,
                repCount = repCount,
                reps = reps.toList(),
                currentPose = result
            )
            is PoseDetectionResult.Error -> WorkoutState(
                formFeedback = "Error: ${result.message}",
                poseQualityScore = 0f,
                repCount = repCount,
                reps = reps.toList(),
                currentPose = result
            )
        }
    }

    private fun processSuccessfulPose(pose: PoseDetectionResult.Success): WorkoutState {
        val exercise = currentExercise ?: ExerciseFactory.create(currentExerciseType).also {
            currentExercise = it
        }

        // Analyze pose quality with exercise-specific analysis
        val quality = PoseQualityAnalyzer.analyzeExerciseSpecificQuality(
            landmarks = pose.landmarks,
            exerciseType = currentExerciseType
        )

        // Generate form feedback
        val feedback = exercise.analyzeFeedback(pose)

        // Record pose frame for analytics (if session is active)
        if (isSessionActive) {
            sessionBuilder?.addPoseFrame(pose, if (repCount > 0) repCount else null)
        }

        // Update rep count
        val repCompleted = exercise.updateRepCount(pose)
        if (repCompleted) {
            repCount++
            // Use weighted form score from exercise-specific validation
            val exerciseFormScore = exercise.formScore()
            val hasGoodPoseQuality = quality.overallConfidence >= MIN_POSE_QUALITY
            // Blend pose quality with exercise form score
            val blendedScore = if (hasGoodPoseQuality) {
                exerciseFormScore
            } else {
                // Reduce score if pose quality is low (landmarks not reliable)
                exerciseFormScore * (quality.overallConfidence / MIN_POSE_QUALITY).coerceIn(0.5f, 1f)
            }
            val isGoodForm = blendedScore >= 60f

            val repData = RepData(
                repNumber = repCount,
                timestamp = System.currentTimeMillis(),
                poseQuality = quality.overallConfidence,
                isGoodForm = isGoodForm,
                formScore = blendedScore
            )

            reps.add(repData)

            // Record rep for analytics (if session is active)
            if (isSessionActive) {
                sessionBuilder?.addRep(repData)
            }
        }

        return WorkoutState(
            formFeedback = feedback,
            poseQualityScore = quality.overallConfidence,
            repCount = repCount,
            reps = reps.toList(),
            currentPose = pose,
            poseQuality = quality
        )
    }

    /**
     * Reset workout state
     */
    fun reset() {
        currentExercise?.reset()
        reps.clear()
        repCount = 0
    }

    /**
     * Get current rep statistics
     */
    fun getRepStats(): RepStats {
        val goodReps = reps.count { it.isGoodForm }
        val badReps = reps.count { !it.isGoodForm }
        return RepStats(
            total = repCount,
            good = goodReps,
            bad = badReps
        )
    }

    /**
     * Get the current exercise instance for phase detection
     */
    fun getCurrentExercise(): Exercise? = currentExercise
}

/**
 * Data class representing the current workout state
 */
data class WorkoutState(
    val formFeedback: String,
    val poseQualityScore: Float,
    val repCount: Int,
    val reps: List<RepData>,
    val currentPose: PoseDetectionResult,
    val poseQuality: PoseQuality? = null
)

/**
 * Data class for rep statistics
 */
data class RepStats(
    val total: Int,
    val good: Int,
    val bad: Int
)
