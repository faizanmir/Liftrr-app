package org.liftrr.ml

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

/**
 * Camera framing feedback
 */
enum class FramingFeedback {
    TOO_CLOSE,
    TOO_FAR,
    GOOD_DISTANCE,
    MOVE_LEFT,
    MOVE_RIGHT,
    MOVE_UP,
    MOVE_DOWN,
    WELL_FRAMED
}

/**
 * Camera angle detection
 */
enum class CameraAngle {
    FRONT_VIEW,
    SIDE_VIEW,
    ANGLED_VIEW,
    UNKNOWN
}

/**
 * Lighting quality assessment
 */
enum class LightingQuality {
    TOO_DARK,
    ACCEPTABLE,
    GOOD
}

/**
 * Pose quality assessment result
 *
 * @property overallConfidence Average visibility confidence across key landmarks (0.0-1.0)
 * @property isFullBodyVisible Whether at least 80% of key landmarks are visible
 * @property missingKeypoints List of landmark names that are not visible
 * @property assessment Human-readable quality rating
 * @property framingFeedback Camera framing suggestions
 * @property cameraAngle Detected camera angle
 * @property lightingQuality Lighting condition assessment
 * @property actionableFeedback List of specific instructions to improve quality
 */
data class PoseQuality(
    val overallConfidence: Float,
    val isFullBodyVisible: Boolean,
    val missingKeypoints: List<String>,
    val assessment: String,
    val framingFeedback: FramingFeedback = FramingFeedback.WELL_FRAMED,
    val cameraAngle: CameraAngle = CameraAngle.UNKNOWN,
    val lightingQuality: LightingQuality = LightingQuality.ACCEPTABLE,
    val actionableFeedback: List<String> = emptyList()
)

/**
 * Analyzer for pose detection quality
 *
 * Evaluates how well the pose is captured based on landmark visibility.
 */
object PoseQualityAnalyzer {

    /**
     * Key landmarks required for workout form analysis
     */
    private val KEY_LANDMARKS = listOf(
        PoseLandmarks.LEFT_SHOULDER,
        PoseLandmarks.RIGHT_SHOULDER,
        PoseLandmarks.LEFT_ELBOW,
        PoseLandmarks.RIGHT_ELBOW,
        PoseLandmarks.LEFT_WRIST,
        PoseLandmarks.RIGHT_WRIST,
        PoseLandmarks.LEFT_HIP,
        PoseLandmarks.RIGHT_HIP,
        PoseLandmarks.LEFT_KNEE,
        PoseLandmarks.RIGHT_KNEE,
        PoseLandmarks.LEFT_ANKLE,
        PoseLandmarks.RIGHT_ANKLE
    )

    /**
     * Get exercise-specific required landmarks
     */
    private fun getRequiredLandmarksForExercise(exerciseType: ExerciseType): List<Int> {
        return when (exerciseType) {
            ExerciseType.SQUAT -> listOf(
                PoseLandmarks.LEFT_HIP,
                PoseLandmarks.RIGHT_HIP,
                PoseLandmarks.LEFT_KNEE,
                PoseLandmarks.RIGHT_KNEE,
                PoseLandmarks.LEFT_ANKLE,
                PoseLandmarks.RIGHT_ANKLE,
                PoseLandmarks.LEFT_SHOULDER,
                PoseLandmarks.RIGHT_SHOULDER
            )
            ExerciseType.DEADLIFT -> KEY_LANDMARKS // Full body needed
            ExerciseType.BENCH_PRESS -> listOf(
                PoseLandmarks.LEFT_SHOULDER,
                PoseLandmarks.RIGHT_SHOULDER,
                PoseLandmarks.LEFT_ELBOW,
                PoseLandmarks.RIGHT_ELBOW,
                PoseLandmarks.LEFT_WRIST,
                PoseLandmarks.RIGHT_WRIST,
                PoseLandmarks.LEFT_HIP,
                PoseLandmarks.RIGHT_HIP
            )
        }
    }

    /**
     * Get recommended camera angle for exercise
     */
    private fun getRecommendedAngle(exerciseType: ExerciseType): CameraAngle {
        return when (exerciseType) {
            ExerciseType.SQUAT -> CameraAngle.SIDE_VIEW
            ExerciseType.DEADLIFT -> CameraAngle.SIDE_VIEW
            ExerciseType.BENCH_PRESS -> CameraAngle.SIDE_VIEW
        }
    }

    /**
     * Visibility thresholds for quality assessment
     */
    private const val THRESHOLD_EXCELLENT = 0.8f
    private const val THRESHOLD_GOOD = 0.6f
    private const val THRESHOLD_FAIR = 0.4f
    private const val THRESHOLD_FULL_BODY = 0.8f  // 80% of landmarks visible

    /**
     * Analyze pose detection quality (generic)
     *
     * @param landmarks List of detected normalized landmarks
     * @return PoseQuality assessment
     */
    fun analyzePoseQuality(landmarks: List<NormalizedLandmark>): PoseQuality {
        val visibleCount = countVisibleLandmarks(landmarks)
        val overallConfidence = calculateAverageConfidence(landmarks)
        val missingKeypoints = findMissingKeypoints(landmarks)
        val isFullBodyVisible = isFullBodyVisible(visibleCount)
        val assessment = assessQuality(overallConfidence, visibleCount)

        return PoseQuality(
            overallConfidence = overallConfidence,
            isFullBodyVisible = isFullBodyVisible,
            missingKeypoints = missingKeypoints,
            assessment = assessment
        )
    }

    /**
     * Analyze pose quality for specific exercise with enhanced feedback
     *
     * @param landmarks List of detected normalized landmarks
     * @param exerciseType Type of exercise being performed
     * @return Enhanced PoseQuality assessment with actionable feedback
     */
    fun analyzeExerciseSpecificQuality(
        landmarks: List<NormalizedLandmark>,
        exerciseType: ExerciseType
    ): PoseQuality {
        val requiredLandmarks = getRequiredLandmarksForExercise(exerciseType)

        val visibleCount = countVisibleLandmarks(landmarks, requiredLandmarks)
        val overallConfidence = calculateAverageConfidence(landmarks, requiredLandmarks)
        val missingKeypoints = findMissingKeypoints(landmarks, requiredLandmarks)
        val isFullBodyVisible = isFullBodyVisible(visibleCount, requiredLandmarks.size)
        val assessment = assessQuality(overallConfidence, visibleCount, requiredLandmarks.size)

        // Enhanced analysis
        val framingFeedback = analyzeFraming(landmarks)
        val cameraAngle = detectCameraAngle(landmarks)
        val lightingQuality = analyzeLighting(landmarks)
        val actionableFeedback = generateActionableFeedback(
            missingKeypoints = missingKeypoints,
            framingFeedback = framingFeedback,
            cameraAngle = cameraAngle,
            lightingQuality = lightingQuality,
            exerciseType = exerciseType,
            overallConfidence = overallConfidence
        )

        return PoseQuality(
            overallConfidence = overallConfidence,
            isFullBodyVisible = isFullBodyVisible,
            missingKeypoints = missingKeypoints,
            assessment = assessment,
            framingFeedback = framingFeedback,
            cameraAngle = cameraAngle,
            lightingQuality = lightingQuality,
            actionableFeedback = actionableFeedback
        )
    }

    /**
     * Count visible key landmarks
     */
    private fun countVisibleLandmarks(
        landmarks: List<NormalizedLandmark>,
        requiredLandmarks: List<Int> = KEY_LANDMARKS
    ): Int {
        return requiredLandmarks.count { index ->
            landmarks.getOrNull(index)?.isVisible() == true
        }
    }

    /**
     * Calculate average visibility confidence
     */
    private fun calculateAverageConfidence(
        landmarks: List<NormalizedLandmark>,
        requiredLandmarks: List<Int> = KEY_LANDMARKS
    ): Float {
        val visibilities = requiredLandmarks.mapNotNull { index ->
            landmarks.getOrNull(index)?.visibility()?.orElse(null)
        }
        return if (visibilities.isNotEmpty()) {
            visibilities.average().toFloat()
        } else {
            0f
        }
    }

    /**
     * Find missing key landmarks
     */
    private fun findMissingKeypoints(
        landmarks: List<NormalizedLandmark>,
        requiredLandmarks: List<Int> = KEY_LANDMARKS
    ): List<String> {
        return requiredLandmarks
            .filter { index ->
                landmarks.getOrNull(index)?.isVisible() != true
            }
            .map { getLandmarkName(it) }
    }

    /**
     * Check if full body is visible
     */
    private fun isFullBodyVisible(
        visibleCount: Int,
        totalRequired: Int = KEY_LANDMARKS.size
    ): Boolean {
        return visibleCount >= totalRequired * THRESHOLD_FULL_BODY
    }

    /**
     * Assess overall quality rating
     */
    private fun assessQuality(
        confidence: Float,
        visibleCount: Int,
        totalRequired: Int = KEY_LANDMARKS.size
    ): String {
        return when {
            confidence >= THRESHOLD_EXCELLENT && visibleCount >= totalRequired * THRESHOLD_FULL_BODY ->
                "Excellent"
            confidence >= THRESHOLD_GOOD && visibleCount >= totalRequired * THRESHOLD_GOOD ->
                "Good"
            confidence >= THRESHOLD_FAIR ->
                "Fair"
            else ->
                "Poor"
        }
    }

    /**
     * Analyze camera framing
     */
    private fun analyzeFraming(landmarks: List<NormalizedLandmark>): FramingFeedback {
        if (landmarks.isEmpty()) return FramingFeedback.TOO_FAR

        // Calculate bounding box of visible landmarks
        val visibleLandmarks = landmarks.filter { it.isVisible() }
        if (visibleLandmarks.isEmpty()) return FramingFeedback.TOO_FAR

        val minX = visibleLandmarks.minOf { it.x() }
        val maxX = visibleLandmarks.maxOf { it.x() }
        val minY = visibleLandmarks.minOf { it.y() }
        val maxY = visibleLandmarks.maxOf { it.y() }

        val width = maxX - minX
        val height = maxY - minY
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f

        // Check if person is too close (fills > 90% of frame)
        if (width > 0.9f || height > 0.9f) {
            return FramingFeedback.TOO_CLOSE
        }

        // Check if person is too far (fills < 40% of frame)
        if (width < 0.4f || height < 0.4f) {
            return FramingFeedback.TOO_FAR
        }

        // Check horizontal centering (should be within 0.3-0.7)
        if (centerX < 0.3f) {
            return FramingFeedback.MOVE_RIGHT
        }
        if (centerX > 0.7f) {
            return FramingFeedback.MOVE_LEFT
        }

        // Check vertical centering
        if (centerY < 0.15f) {
            return FramingFeedback.MOVE_DOWN
        }
        if (centerY > 0.4f && maxY > 0.95f) {
            return FramingFeedback.MOVE_UP
        }

        // Check if well-framed (fills 50-80% of frame and centered)
        if (width in 0.5f..0.8f && height in 0.5f..0.8f &&
            centerX in 0.35f..0.65f && centerY in 0.2f..0.5f) {
            return FramingFeedback.WELL_FRAMED
        }

        return FramingFeedback.GOOD_DISTANCE
    }

    /**
     * Detect camera angle based on landmark positions
     */
    private fun detectCameraAngle(landmarks: List<NormalizedLandmark>): CameraAngle {
        val leftShoulder = landmarks.getOrNull(PoseLandmarks.LEFT_SHOULDER)
        val rightShoulder = landmarks.getOrNull(PoseLandmarks.RIGHT_SHOULDER)

        if (leftShoulder?.isVisible() != true || rightShoulder?.isVisible() != true) {
            return CameraAngle.UNKNOWN
        }

        // Calculate shoulder width in x-axis (normalized coordinates)
        val shoulderWidth = kotlin.math.abs(rightShoulder.x() - leftShoulder.x())

        // Side view: shoulders appear close together (< 0.1)
        // Front view: shoulders appear far apart (> 0.2)
        return when {
            shoulderWidth < 0.1f -> CameraAngle.SIDE_VIEW
            shoulderWidth > 0.25f -> CameraAngle.FRONT_VIEW
            else -> CameraAngle.ANGLED_VIEW
        }
    }

    /**
     * Analyze lighting quality based on confidence scores
     */
    private fun analyzeLighting(landmarks: List<NormalizedLandmark>): LightingQuality {
        val avgConfidence = calculateAverageConfidence(landmarks)
        return when {
            avgConfidence < 0.4f -> LightingQuality.TOO_DARK
            avgConfidence >= 0.7f -> LightingQuality.GOOD
            else -> LightingQuality.ACCEPTABLE
        }
    }

    /**
     * Generate actionable feedback for the user
     */
    private fun generateActionableFeedback(
        missingKeypoints: List<String>,
        framingFeedback: FramingFeedback,
        cameraAngle: CameraAngle,
        lightingQuality: LightingQuality,
        exerciseType: ExerciseType,
        overallConfidence: Float
    ): List<String> {
        val feedback = mutableListOf<String>()

        // Lighting feedback (highest priority)
        when (lightingQuality) {
            LightingQuality.TOO_DARK -> feedback.add("Turn on more lights or move to brighter area")
            LightingQuality.ACCEPTABLE -> if (overallConfidence < 0.6f) {
                feedback.add("Improve lighting for better accuracy")
            }
            LightingQuality.GOOD -> {} // No feedback needed
        }

        // Framing feedback
        when (framingFeedback) {
            FramingFeedback.TOO_CLOSE -> feedback.add("Step back from camera")
            FramingFeedback.TOO_FAR -> feedback.add("Move closer to camera")
            FramingFeedback.MOVE_LEFT -> feedback.add("Move to the left")
            FramingFeedback.MOVE_RIGHT -> feedback.add("Move to the right")
            FramingFeedback.MOVE_UP -> feedback.add("Raise camera higher")
            FramingFeedback.MOVE_DOWN -> feedback.add("Lower camera")
            FramingFeedback.GOOD_DISTANCE -> {} // Close enough
            FramingFeedback.WELL_FRAMED -> {} // Perfect
        }

        // Camera angle feedback
        val recommendedAngle = getRecommendedAngle(exerciseType)
        if (cameraAngle != CameraAngle.UNKNOWN && cameraAngle != recommendedAngle) {
            when (recommendedAngle) {
                CameraAngle.SIDE_VIEW -> feedback.add("Position camera to your side for best form analysis")
                CameraAngle.FRONT_VIEW -> feedback.add("Face the camera directly")
                else -> {}
            }
        }

        // Missing body parts feedback
        when {
            missingKeypoints.any { it.contains("Ankle") || it.contains("Knee") } -> {
                feedback.add("Show full legs in frame")
            }
            missingKeypoints.any { it.contains("Hip") } -> {
                feedback.add("Lower camera to see hips")
            }
            missingKeypoints.any { it.contains("Shoulder") } -> {
                feedback.add("Show upper body in frame")
            }
            missingKeypoints.any { it.contains("Wrist") || it.contains("Elbow") } -> {
                if (exerciseType == ExerciseType.BENCH_PRESS) {
                    feedback.add("Show full arms in frame")
                }
            }
        }

        // If everything is good, provide encouragement
        if (feedback.isEmpty() && framingFeedback == FramingFeedback.WELL_FRAMED) {
            feedback.add("Perfect setup - ready to start!")
        }

        return feedback.take(2) // Limit to 2 most important suggestions
    }

    /**
     * Get human-readable landmark name
     */
    private fun getLandmarkName(index: Int): String = when (index) {
        PoseLandmarks.LEFT_SHOULDER -> "Left Shoulder"
        PoseLandmarks.RIGHT_SHOULDER -> "Right Shoulder"
        PoseLandmarks.LEFT_ELBOW -> "Left Elbow"
        PoseLandmarks.RIGHT_ELBOW -> "Right Elbow"
        PoseLandmarks.LEFT_WRIST -> "Left Wrist"
        PoseLandmarks.RIGHT_WRIST -> "Right Wrist"
        PoseLandmarks.LEFT_HIP -> "Left Hip"
        PoseLandmarks.RIGHT_HIP -> "Right Hip"
        PoseLandmarks.LEFT_KNEE -> "Left Knee"
        PoseLandmarks.RIGHT_KNEE -> "Right Knee"
        PoseLandmarks.LEFT_ANKLE -> "Left Ankle"
        PoseLandmarks.RIGHT_ANKLE -> "Right Ankle"
        else -> "Unknown"
    }
}
