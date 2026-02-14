package org.liftrr.domain.analytics

import org.liftrr.domain.workout.RepData
import org.liftrr.ml.ExerciseType
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ui.screens.session.WorkoutMode

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
    val durationMs: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime

    val totalReps: Int
        get() = reps.size

    val goodReps: Int
        get() = reps.count { it.isGoodForm }

    val badReps: Int
        get() = reps.size - goodReps

    val averageQuality: Float
        get() = if (reps.isEmpty()) 0f else reps.map { it.poseQuality }.average().toFloat()
}

data class PoseFrame(
    val timestamp: Long,
    val frameNumber: Int,
    val poseResult: PoseDetectionResult,
    val repNumber: Int? = null
)

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

    fun addRep(rep: RepData) {
        reps.add(rep)
    }

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

    fun setVideoPath(path: String) {
        videoPath = path
    }

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
