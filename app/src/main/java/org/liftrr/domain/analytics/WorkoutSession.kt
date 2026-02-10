package org.liftrr.domain.analytics

import org.liftrr.domain.workout.RepData
import org.liftrr.ml.ExerciseType
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ui.screens.session.WorkoutMode

/**
 * Represents a complete workout session with all collected data
 *
 * This is the primary data structure for workout analytics and reporting.
 * Contains all pose detection results, rep data, and metadata needed for analysis.
 */
data class WorkoutSession(
    val id: String,
    val exerciseType: ExerciseType,
    val workoutMode: WorkoutMode,
    val startTime: Long,
    val endTime: Long? = null,
    val reps: List<RepData> = emptyList(),
    val poseFrames: List<PoseFrame> = emptyList(),
    val videoPath: String? = null,
    val completed: Boolean = false
) {
    /**
     * Calculate total workout duration in milliseconds
     */
    val durationMs: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime

    /**
     * Get total rep count
     */
    val totalReps: Int
        get() = reps.size

    /**
     * Get good form rep count
     */
    val goodReps: Int
        get() = reps.count { it.isGoodForm }

    /**
     * Get bad form rep count
     */
    val badReps: Int
        get() = reps.size - goodReps

    /**
     * Get average pose quality score across all reps
     */
    val averageQuality: Float
        get() = if (reps.isEmpty()) 0f else reps.map { it.poseQuality }.average().toFloat()
}

/**
 * Represents a single frame of pose detection data
 *
 * Stores the pose detection result along with timing information.
 * Used for detailed analysis and video synchronization.
 */
data class PoseFrame(
    val timestamp: Long,
    val frameNumber: Int,
    val poseResult: PoseDetectionResult,
    val repNumber: Int? = null // Which rep this frame belongs to (if any)
)

/**
 * Builder for WorkoutSession to collect data during workout
 */
class WorkoutSessionBuilder(
    private val exerciseType: ExerciseType,
    private val workoutMode: WorkoutMode
) {
    private val id = generateSessionId()
    private val startTime = System.currentTimeMillis()
    private val reps = mutableListOf<RepData>()
    private val poseFrames = mutableListOf<PoseFrame>()
    private var videoPath: String? = null
    private var frameCounter = 0

    /**
     * Add a rep to the session
     */
    fun addRep(rep: RepData) {
        reps.add(rep)
    }

    /**
     * Add a pose detection frame
     */
    fun addPoseFrame(poseResult: PoseDetectionResult, currentRepNumber: Int? = null) {
        poseFrames.add(
            PoseFrame(
                timestamp = System.currentTimeMillis(),
                frameNumber = frameCounter++,
                poseResult = poseResult,
                repNumber = currentRepNumber
            )
        )
    }

    /**
     * Set video path for the session
     */
    fun setVideoPath(path: String) {
        videoPath = path
    }

    /**
     * Build the final WorkoutSession
     */
    fun build(completed: Boolean = true): WorkoutSession {
        return WorkoutSession(
            id = id,
            exerciseType = exerciseType,
            workoutMode = workoutMode,
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            reps = reps.toList(),
            poseFrames = poseFrames.toList(),
            videoPath = videoPath,
            completed = completed
        )
    }

    companion object {
        private fun generateSessionId(): String {
            return "session_${System.currentTimeMillis()}_${(0..9999).random()}"
        }
    }
}
