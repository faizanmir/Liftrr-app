package org.liftrr.domain.analytics

import org.liftrr.domain.workout.RepData
import org.liftrr.ml.ExerciseType
import org.liftrr.ml.PoseAnalyzer
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseLandmarks
import kotlin.math.abs

/**
 * Analyzes workout session data to extract insights and metrics
 *
 * Provides detailed analysis of:
 * - Range of Motion (ROM)
 * - Tempo and timing
 * - Form consistency
 * - Left/right symmetry
 * - Movement patterns
 */
object WorkoutAnalyzer {

    /**
     * Analyze a complete workout session
     */
    fun analyzeSession(session: WorkoutSession): WorkoutReport {
        val repAnalyses = analyzeAllReps(session)
        val rangeOfMotion = analyzeRangeOfMotion(session)
        val tempo = analyzeTempo(session)
        val symmetry = analyzeSymmetry(session)
        val formConsistency = analyzeFormConsistency(session)
        val exerciseSpecificMetrics = analyzeExerciseSpecificMetrics(session)

        return WorkoutReport(
            sessionId = session.id,
            exerciseType = session.exerciseType,
            totalReps = session.totalReps,
            goodReps = session.goodReps,
            badReps = session.badReps,
            averageQuality = session.averageQuality,
            durationMs = session.durationMs,
            rangeOfMotion = rangeOfMotion,
            tempo = tempo,
            symmetry = symmetry,
            formConsistency = formConsistency,
            repAnalyses = repAnalyses,
            recommendations = generateRecommendations(
                session, repAnalyses, rangeOfMotion, tempo, symmetry, exerciseSpecificMetrics
            ),
            exerciseSpecificMetrics = exerciseSpecificMetrics
        )
    }

    /**
     * Analyze all reps individually
     */
    private fun analyzeAllReps(session: WorkoutSession): List<RepAnalysis> {
        return session.reps.mapIndexed { index, rep ->
            // Get all pose frames for this rep
            val repFrames = session.poseFrames.filter { it.repNumber == rep.repNumber }
            analyzeRep(rep, repFrames, session.exerciseType)
        }
    }

    /**
     * Analyze a single rep in detail
     */
    private fun analyzeRep(
        rep: RepData,
        frames: List<PoseFrame>,
        exerciseType: ExerciseType
    ): RepAnalysis {
        val successFrames = frames.mapNotNull {
            (it.poseResult as? PoseDetectionResult.Success)
        }

        if (successFrames.isEmpty()) {
            return RepAnalysis(
                repNumber = rep.repNumber,
                isGoodForm = rep.isGoodForm,
                quality = rep.poseQuality,
                depth = null,
                tempo = null,
                peakFrameIndex = null,
                formIssues = listOf("Insufficient data")
            )
        }

        // Calculate depth (range of motion for this rep)
        val depth = calculateRepDepth(successFrames, exerciseType)

        // Calculate tempo (time from start to peak to finish)
        val tempo = calculateRepTempo(frames)

        // Find peak frame (deepest point)
        val peakFrameIndex = findPeakFrame(successFrames, exerciseType)

        // Identify form issues
        val formIssues = identifyFormIssues(successFrames, exerciseType)

        return RepAnalysis(
            repNumber = rep.repNumber,
            isGoodForm = rep.isGoodForm,
            quality = rep.poseQuality,
            depth = depth,
            tempo = tempo,
            peakFrameIndex = peakFrameIndex,
            formIssues = formIssues
        )
    }

    /**
     * Calculate depth/range of motion for a rep
     */
    private fun calculateRepDepth(
        frames: List<PoseDetectionResult.Success>,
        exerciseType: ExerciseType
    ): Float? {
        return when (exerciseType) {
            ExerciseType.SQUAT -> {
                // For squats, measure hip angle (smaller = deeper)
                val hipAngles = frames.mapNotNull { pose ->
                    val leftShoulder = pose.landmarks.getOrNull(PoseLandmarks.LEFT_SHOULDER)
                    val leftHip = pose.landmarks.getOrNull(PoseLandmarks.LEFT_HIP)
                    val leftKnee = pose.landmarks.getOrNull(PoseLandmarks.LEFT_KNEE)

                    if (leftShoulder != null && leftHip != null && leftKnee != null) {
                        PoseAnalyzer.calculateAngle(leftShoulder, leftHip, leftKnee)
                    } else null
                }
                hipAngles.minOrNull() // Minimum angle = deepest squat
            }
            ExerciseType.DEADLIFT -> {
                // For deadlifts, measure hip angle (smaller at bottom, larger at top)
                val hipAngles = frames.mapNotNull { pose ->
                    val leftShoulder = pose.landmarks.getOrNull(PoseLandmarks.LEFT_SHOULDER)
                    val leftHip = pose.landmarks.getOrNull(PoseLandmarks.LEFT_HIP)
                    val leftKnee = pose.landmarks.getOrNull(PoseLandmarks.LEFT_KNEE)

                    if (leftShoulder != null && leftHip != null && leftKnee != null) {
                        PoseAnalyzer.calculateAngle(leftShoulder, leftHip, leftKnee)
                    } else null
                }
                // Range = difference between max and min
                val max = hipAngles.maxOrNull() ?: return null
                val min = hipAngles.minOrNull() ?: return null
                max - min
            }
            ExerciseType.BENCH_PRESS -> {
                // For bench press, measure elbow angle (smaller = deeper)
                val elbowAngles = frames.mapNotNull { pose ->
                    val leftShoulder = pose.landmarks.getOrNull(PoseLandmarks.LEFT_SHOULDER)
                    val leftElbow = pose.landmarks.getOrNull(PoseLandmarks.LEFT_ELBOW)
                    val leftWrist = pose.landmarks.getOrNull(PoseLandmarks.LEFT_WRIST)

                    if (leftShoulder != null && leftElbow != null && leftWrist != null) {
                        PoseAnalyzer.calculateAngle(leftShoulder, leftElbow, leftWrist)
                    } else null
                }
                elbowAngles.minOrNull()
            }
        }
    }

    /**
     * Calculate tempo (time distribution) for a rep
     */
    private fun calculateRepTempo(frames: List<PoseFrame>): RepTempo? {
        if (frames.size < 3) return null

        val startTime = frames.first().timestamp
        val endTime = frames.last().timestamp
        val totalDuration = endTime - startTime

        // Find the peak (deepest point) timestamp
        // For now, assume it's roughly in the middle
        val peakIndex = frames.size / 2
        val peakTime = frames[peakIndex].timestamp

        val eccentricDuration = peakTime - startTime  // Going down
        val concentricDuration = endTime - peakTime   // Coming up

        return RepTempo(
            totalMs = totalDuration,
            eccentricMs = eccentricDuration,
            concentricMs = concentricDuration
        )
    }

    /**
     * Find the frame index with the deepest position
     */
    private fun findPeakFrame(
        frames: List<PoseDetectionResult.Success>,
        exerciseType: ExerciseType
    ): Int? {
        // For now, return middle frame as approximation
        return frames.size / 2
    }

    /**
     * Identify specific form issues in a rep
     */
    private fun identifyFormIssues(
        frames: List<PoseDetectionResult.Success>,
        exerciseType: ExerciseType
    ): List<String> {
        val issues = mutableListOf<String>()

        when (exerciseType) {
            ExerciseType.SQUAT -> {
                // Check for common squat issues
                // Example: knees caving in, not going deep enough, etc.
            }
            ExerciseType.DEADLIFT -> {
                // Check for deadlift issues
                // Example: rounded back, not locking out, etc.
            }
            ExerciseType.BENCH_PRESS -> {
                // Check for bench press issues
            }
        }

        return issues
    }

    /**
     * Analyze overall range of motion across all reps
     */
    private fun analyzeRangeOfMotion(session: WorkoutSession): RangeOfMotionAnalysis {
        val successFrames = session.poseFrames.mapNotNull {
            it.poseResult as? PoseDetectionResult.Success
        }

        if (successFrames.isEmpty()) {
            return RangeOfMotionAnalysis(
                averageDepth = 0f,
                minDepth = 0f,
                maxDepth = 0f,
                consistency = 0f
            )
        }

        // Calculate depth for each rep
        val repDepths = session.reps.mapNotNull { rep ->
            val repFrames = session.poseFrames
                .filter { it.repNumber == rep.repNumber }
                .mapNotNull { it.poseResult as? PoseDetectionResult.Success }
            calculateRepDepth(repFrames, session.exerciseType)
        }

        if (repDepths.isEmpty()) {
            return RangeOfMotionAnalysis(
                averageDepth = 0f,
                minDepth = 0f,
                maxDepth = 0f,
                consistency = 0f
            )
        }

        val average = repDepths.average().toFloat()
        val min = repDepths.minOrNull() ?: 0f
        val max = repDepths.maxOrNull() ?: 0f

        // Consistency = how close reps are to each other (lower variance = higher consistency)
        val variance = repDepths.map { (it - average) * (it - average) }.average()
        val consistency = 100f - (variance.toFloat() / average * 100).coerceIn(0f, 100f)

        return RangeOfMotionAnalysis(
            averageDepth = average,
            minDepth = min,
            maxDepth = max,
            consistency = consistency
        )
    }

    /**
     * Analyze tempo/timing patterns
     */
    private fun analyzeTempo(session: WorkoutSession): TempoAnalysis {
        val repDurations = mutableListOf<Long>()
        var previousRepTime: Long? = null

        session.reps.forEach { rep ->
            if (previousRepTime != null) {
                repDurations.add(rep.timestamp - previousRepTime!!)
            }
            previousRepTime = rep.timestamp
        }

        if (repDurations.isEmpty()) {
            return TempoAnalysis(
                averageRepDurationMs = 0L,
                averageRestBetweenRepsMs = 0L,
                tempoConsistency = 0f
            )
        }

        val average = repDurations.average().toLong()
        val variance = repDurations.map { (it - average) * (it - average) }.average()
        val consistency = 100f - (variance / average * 100).coerceIn(0.0, 100.0).toFloat()

        return TempoAnalysis(
            averageRepDurationMs = average,
            averageRestBetweenRepsMs = average,
            tempoConsistency = consistency
        )
    }

    /**
     * Analyze left/right symmetry
     */
    private fun analyzeSymmetry(session: WorkoutSession): SymmetryAnalysis {
        val successFrames = session.poseFrames.mapNotNull {
            it.poseResult as? PoseDetectionResult.Success
        }

        if (successFrames.isEmpty()) {
            return SymmetryAnalysis(
                overallSymmetry = 0f,
                leftRightAngleDifference = 0f,
                issues = emptyList()
            )
        }

        // Calculate symmetry by comparing left vs right angles
        val symmetryScores = successFrames.mapNotNull { pose ->
            calculateSymmetryScore(pose, session.exerciseType)
        }

        val averageSymmetry = if (symmetryScores.isNotEmpty()) {
            symmetryScores.average().toFloat()
        } else 0f

        return SymmetryAnalysis(
            overallSymmetry = averageSymmetry,
            leftRightAngleDifference = 100f - averageSymmetry,
            issues = if (averageSymmetry < 80f) listOf("Uneven weight distribution") else emptyList()
        )
    }

    /**
     * Calculate symmetry score for a single pose
     */
    private fun calculateSymmetryScore(
        pose: PoseDetectionResult.Success,
        exerciseType: ExerciseType
    ): Float? {
        val leftShoulder = pose.landmarks.getOrNull(PoseLandmarks.LEFT_SHOULDER)
        val leftHip = pose.landmarks.getOrNull(PoseLandmarks.LEFT_HIP)
        val leftKnee = pose.landmarks.getOrNull(PoseLandmarks.LEFT_KNEE)
        val rightShoulder = pose.landmarks.getOrNull(PoseLandmarks.RIGHT_SHOULDER)
        val rightHip = pose.landmarks.getOrNull(PoseLandmarks.RIGHT_HIP)
        val rightKnee = pose.landmarks.getOrNull(PoseLandmarks.RIGHT_KNEE)

        if (leftShoulder == null || leftHip == null || leftKnee == null ||
            rightShoulder == null || rightHip == null || rightKnee == null) {
            return null
        }

        return when (exerciseType) {
            ExerciseType.SQUAT, ExerciseType.DEADLIFT -> {
                // For squats and deadlifts, compare hip angles
                val leftHipAngle = PoseAnalyzer.calculateAngle(leftShoulder, leftHip, leftKnee)
                val rightHipAngle = PoseAnalyzer.calculateAngle(rightShoulder, rightHip, rightKnee)

                // Symmetry = 100 - percentage difference
                val difference = abs(leftHipAngle - rightHipAngle)
                100f - (difference / leftHipAngle.coerceAtLeast(1f) * 100f).coerceIn(0f, 100f)
            }
            ExerciseType.BENCH_PRESS -> {
                // For bench press, compare elbow angles
                val leftElbow = pose.landmarks.getOrNull(PoseLandmarks.LEFT_ELBOW)
                val leftWrist = pose.landmarks.getOrNull(PoseLandmarks.LEFT_WRIST)
                val rightElbow = pose.landmarks.getOrNull(PoseLandmarks.RIGHT_ELBOW)
                val rightWrist = pose.landmarks.getOrNull(PoseLandmarks.RIGHT_WRIST)

                if (leftElbow == null || leftWrist == null || rightElbow == null || rightWrist == null) {
                    return null
                }

                val leftElbowAngle = PoseAnalyzer.calculateAngle(leftShoulder, leftElbow, leftWrist)
                val rightElbowAngle = PoseAnalyzer.calculateAngle(rightShoulder, rightElbow, rightWrist)

                val difference = abs(leftElbowAngle - rightElbowAngle)
                100f - (difference / leftElbowAngle.coerceAtLeast(1f) * 100f).coerceIn(0f, 100f)
            }
        }
    }

    /**
     * Analyze form consistency across reps
     */
    private fun analyzeFormConsistency(session: WorkoutSession): FormConsistencyAnalysis {
        val qualityScores = session.reps.map { it.poseQuality }

        if (qualityScores.isEmpty()) {
            return FormConsistencyAnalysis(
                consistencyScore = 0f,
                qualityTrend = "N/A"
            )
        }

        val average = qualityScores.average().toFloat()
        val variance = qualityScores.map { (it - average) * (it - average) }.average()
        val consistency = 100f - (variance / average * 100).coerceIn(0.0, 100.0).toFloat()

        // Detect trend (improving, declining, stable)
        val firstHalf = qualityScores.take(qualityScores.size / 2).average()
        val secondHalf = qualityScores.drop(qualityScores.size / 2).average()
        val trend = when {
            secondHalf > firstHalf + 0.05 -> "Improving"
            secondHalf < firstHalf - 0.05 -> "Declining"
            else -> "Stable"
        }

        return FormConsistencyAnalysis(
            consistencyScore = consistency,
            qualityTrend = trend
        )
    }

    /**
     * Analyze exercise-specific metrics based on exercise type
     */
    private fun analyzeExerciseSpecificMetrics(session: WorkoutSession): ExerciseSpecificMetrics? {
        val successFrames = session.poseFrames.mapNotNull {
            it.poseResult as? PoseDetectionResult.Success
        }

        if (successFrames.isEmpty()) return null

        return when (session.exerciseType) {
            ExerciseType.SQUAT -> analyzeSquatMetrics(successFrames, session)
            ExerciseType.DEADLIFT -> analyzeDeadliftMetrics(successFrames, session)
            ExerciseType.BENCH_PRESS -> analyzeBenchPressMetrics(successFrames, session)
        }
    }

    /**
     * Analyze squat-specific metrics
     */
    private fun analyzeSquatMetrics(
        frames: List<PoseDetectionResult.Success>,
        session: WorkoutSession
    ): ExerciseSpecificMetrics.SquatMetrics {
        // Calculate depth (hip angle)
        val hipAngles = frames.mapNotNull { pose ->
            val leftShoulder = pose.landmarks.getOrNull(PoseLandmarks.LEFT_SHOULDER)
            val leftHip = pose.landmarks.getOrNull(PoseLandmarks.LEFT_HIP)
            val leftKnee = pose.landmarks.getOrNull(PoseLandmarks.LEFT_KNEE)

            if (leftShoulder != null && leftHip != null && leftKnee != null) {
                PoseAnalyzer.calculateAngle(leftShoulder, leftHip, leftKnee)
            } else null
        }

        val averageDepth = hipAngles.average().toFloat()
        val depthConsistency = calculateConsistency(hipAngles.map { it.toFloat() })

        // Knee tracking analysis
        val kneeTracking = analyzeKneeTracking(frames)

        // Torso angle analysis
        val torsoAngle = analyzeTorsoAngle(frames)

        // Balance analysis
        val balance = analyzeBalance(frames)

        return ExerciseSpecificMetrics.SquatMetrics(
            averageDepth = averageDepth,
            depthConsistency = depthConsistency,
            kneeTracking = kneeTracking,
            hipMobility = calculateHipMobility(averageDepth),
            torsoAngle = torsoAngle,
            balance = balance
        )
    }

    /**
     * Analyze deadlift-specific metrics
     */
    private fun analyzeDeadliftMetrics(
        frames: List<PoseDetectionResult.Success>,
        session: WorkoutSession
    ): ExerciseSpecificMetrics.DeadliftMetrics {
        val hipHingeQuality = analyzeHipHinge(frames)
        val backAnalysis = analyzeBackStraightness(frames)
        val lockout = analyzeLockoutCompletion(frames)
        val startPos = analyzeStartingPosition(frames)

        return ExerciseSpecificMetrics.DeadliftMetrics(
            hipHingeQuality = hipHingeQuality,
            backStraightness = backAnalysis,
            lockoutCompletion = lockout,
            barPath = null, // Would need additional tracking
            startingPosition = startPos
        )
    }

    /**
     * Analyze bench press-specific metrics
     */
    private fun analyzeBenchPressMetrics(
        frames: List<PoseDetectionResult.Success>,
        session: WorkoutSession
    ): ExerciseSpecificMetrics.BenchPressMetrics {
        val elbowAnalysis = analyzeElbowAngle(frames)
        val shoulderAnalysis = analyzeShoulderPosition(frames)

        return ExerciseSpecificMetrics.BenchPressMetrics(
            barPath = null, // Would need additional tracking
            elbowAngle = elbowAnalysis,
            shoulderPosition = shoulderAnalysis,
            touchPointConsistency = 75f, // Placeholder - would need touch detection
            archMaintenance = 80f // Placeholder - would need torso tracking
        )
    }

    /**
     * Analyze knee tracking for squats
     */
    private fun analyzeKneeTracking(frames: List<PoseDetectionResult.Success>): KneeTrackingAnalysis {
        var kneeCaveCount = 0
        val alignmentScores = mutableListOf<Float>()

        frames.forEach { pose ->
            val leftHip = pose.landmarks.getOrNull(PoseLandmarks.LEFT_HIP)
            val leftKnee = pose.landmarks.getOrNull(PoseLandmarks.LEFT_KNEE)
            val leftAnkle = pose.landmarks.getOrNull(PoseLandmarks.LEFT_ANKLE)
            val rightHip = pose.landmarks.getOrNull(PoseLandmarks.RIGHT_HIP)
            val rightKnee = pose.landmarks.getOrNull(PoseLandmarks.RIGHT_KNEE)
            val rightAnkle = pose.landmarks.getOrNull(PoseLandmarks.RIGHT_ANKLE)

            if (leftHip != null && leftKnee != null && leftAnkle != null &&
                rightHip != null && rightKnee != null && rightAnkle != null) {

                // Check if knees are caving in (knee x-position < ankle x-position)
                val leftKneeCave = leftKnee.x() < leftAnkle.x()
                val rightKneeCave = rightKnee.x() > rightAnkle.x()

                if (leftKneeCave || rightKneeCave) {
                    kneeCaveCount++
                }

                // Calculate alignment score (simplified)
                val leftAlignment = 100f - abs(leftKnee.x() - leftAnkle.x()) * 100f
                val rightAlignment = 100f - abs(rightKnee.x() - rightAnkle.x()) * 100f
                alignmentScores.add((leftAlignment + rightAlignment) / 2f)
            }
        }

        val avgAlignment = if (alignmentScores.isNotEmpty()) {
            alignmentScores.average().toFloat().coerceIn(0f, 100f)
        } else 0f

        val kneeCavePercentage = (kneeCaveCount.toFloat() / frames.size.coerceAtLeast(1)) * 100f

        val issues = mutableListOf<String>()
        if (kneeCavePercentage > 30f) {
            issues.add("Knees caving inward on ${kneeCavePercentage.toInt()}% of reps")
        }
        if (avgAlignment < 70f) {
            issues.add("Inconsistent knee tracking over toes")
        }

        return KneeTrackingAnalysis(
            kneeAlignment = avgAlignment,
            kneeCavePercentage = kneeCavePercentage,
            lateralStability = avgAlignment,
            issues = issues
        )
    }

    /**
     * Analyze torso angle
     */
    private fun analyzeTorsoAngle(frames: List<PoseDetectionResult.Success>): TorsoAngleAnalysis {
        val angles = frames.mapNotNull { pose ->
            val shoulder = pose.landmarks.getOrNull(PoseLandmarks.LEFT_SHOULDER)
            val hip = pose.landmarks.getOrNull(PoseLandmarks.LEFT_HIP)
            val knee = pose.landmarks.getOrNull(PoseLandmarks.LEFT_KNEE)

            if (shoulder != null && hip != null && knee != null) {
                // Calculate torso angle from vertical
                val dy = shoulder.y() - hip.y()
                val dx = shoulder.x() - hip.x()
                Math.toDegrees(Math.atan2(dx.toDouble(), dy.toDouble())).toFloat()
            } else null
        }

        val avgAngle = if (angles.isNotEmpty()) angles.average().toFloat() else 0f
        val consistency = calculateConsistency(angles)

        return TorsoAngleAnalysis(
            averageForwardLean = abs(avgAngle),
            consistency = consistency,
            excessiveLean = abs(avgAngle) > 45f
        )
    }

    /**
     * Analyze balance and stability
     */
    private fun analyzeBalance(frames: List<PoseDetectionResult.Success>): BalanceAnalysis {
        // Simplified balance analysis
        return BalanceAnalysis(
            weightDistribution = 85f, // Would need pressure sensors
            heelPressure = 80f, // Would need pressure sensors
            stability = 75f, // Based on pose consistency
            issues = emptyList()
        )
    }

    /**
     * Analyze hip hinge quality (deadlift)
     */
    private fun analyzeHipHinge(frames: List<PoseDetectionResult.Success>): Float {
        val hingeAngles = frames.mapNotNull { pose ->
            val shoulder = pose.landmarks.getOrNull(PoseLandmarks.LEFT_SHOULDER)
            val hip = pose.landmarks.getOrNull(PoseLandmarks.LEFT_HIP)
            val knee = pose.landmarks.getOrNull(PoseLandmarks.LEFT_KNEE)

            if (shoulder != null && hip != null && knee != null) {
                PoseAnalyzer.calculateAngle(shoulder, hip, knee)
            } else null
        }

        // Good hip hinge has specific angle pattern
        return if (hingeAngles.isNotEmpty()) {
            val avgAngle = hingeAngles.average().toFloat()
            // Score based on proper hip hinge angle range (90-120 degrees)
            when {
                avgAngle in 90f..120f -> 95f
                avgAngle in 80f..130f -> 80f
                else -> 60f
            }
        } else 0f
    }

    /**
     * Analyze back straightness (deadlift)
     */
    private fun analyzeBackStraightness(frames: List<PoseDetectionResult.Success>): BackAnalysis {
        // Simplified - would need more sophisticated spinal tracking
        val issues = mutableListOf<String>()

        return BackAnalysis(
            spineNeutral = 85f,
            lowerBackRounding = 15f,
            upperBackRounding = 10f,
            issues = issues
        )
    }

    /**
     * Analyze lockout completion (deadlift)
     */
    private fun analyzeLockoutCompletion(frames: List<PoseDetectionResult.Success>): Float {
        val hipExtensions = frames.mapNotNull { pose ->
            val shoulder = pose.landmarks.getOrNull(PoseLandmarks.LEFT_SHOULDER)
            val hip = pose.landmarks.getOrNull(PoseLandmarks.LEFT_HIP)
            val knee = pose.landmarks.getOrNull(PoseLandmarks.LEFT_KNEE)

            if (shoulder != null && hip != null && knee != null) {
                PoseAnalyzer.calculateAngle(shoulder, hip, knee)
            } else null
        }

        val maxExtension = hipExtensions.maxOrNull() ?: 0f
        // Full lockout should be around 180 degrees
        return ((maxExtension / 180f) * 100f).coerceIn(0f, 100f)
    }

    /**
     * Analyze starting position (deadlift)
     */
    private fun analyzeStartingPosition(frames: List<PoseDetectionResult.Success>): StartPositionAnalysis {
        // Analyze first few frames for starting position
        val startFrames = frames.take(5)

        return StartPositionAnalysis(
            hipHeight = 80f, // Would need specific tracking
            shoulderPosition = 85f, // Shoulders should be over bar
            setup = 82f,
            issues = emptyList()
        )
    }

    /**
     * Analyze elbow angle (bench press)
     */
    private fun analyzeElbowAngle(frames: List<PoseDetectionResult.Success>): ElbowAngleAnalysis {
        val elbowAngles = frames.mapNotNull { pose ->
            val shoulder = pose.landmarks.getOrNull(PoseLandmarks.LEFT_SHOULDER)
            val elbow = pose.landmarks.getOrNull(PoseLandmarks.LEFT_ELBOW)
            val wrist = pose.landmarks.getOrNull(PoseLandmarks.LEFT_WRIST)

            if (shoulder != null && elbow != null && wrist != null) {
                PoseAnalyzer.calculateAngle(shoulder, elbow, wrist)
            } else null
        }

        val minAngle = elbowAngles.minOrNull() ?: 90f
        val maxAngle = elbowAngles.maxOrNull() ?: 180f
        val consistency = calculateConsistency(elbowAngles)

        return ElbowAngleAnalysis(
            bottomAngle = minAngle,
            topAngle = maxAngle,
            consistency = consistency,
            tucking = 75f // Would need more specific tracking
        )
    }

    /**
     * Analyze shoulder position (bench press)
     */
    private fun analyzeShoulderPosition(frames: List<PoseDetectionResult.Success>): ShoulderAnalysis {
        return ShoulderAnalysis(
            retraction = 80f, // Would need depth camera
            depression = 75f,
            stability = 85f,
            issues = emptyList()
        )
    }

    /**
     * Calculate hip mobility score based on squat depth
     */
    private fun calculateHipMobility(averageDepth: Float): Float {
        // Parallel squat is around 90 degrees
        // Deeper = better mobility
        return when {
            averageDepth < 90f -> 100f // Excellent
            averageDepth < 100f -> 90f  // Very good
            averageDepth < 110f -> 75f  // Good
            averageDepth < 120f -> 60f  // Moderate
            else -> 40f                 // Limited
        }
    }

    /**
     * Calculate consistency score from a list of values
     */
    private fun calculateConsistency(values: List<Float>): Float {
        if (values.isEmpty()) return 0f

        val average = values.average().toFloat()
        val variance = values.map { (it - average) * (it - average) }.average()
        return (100f - (variance.toFloat() / average.coerceAtLeast(1f) * 100f)).coerceIn(0f, 100f)
    }

    /**
     * Generate personalized recommendations
     */
    private fun generateRecommendations(
        session: WorkoutSession,
        repAnalyses: List<RepAnalysis>,
        rom: RangeOfMotionAnalysis,
        tempo: TempoAnalysis,
        symmetry: SymmetryAnalysis,
        exerciseMetrics: ExerciseSpecificMetrics?
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // Form quality recommendations
        if (session.badReps > session.goodReps) {
            recommendations.add("Focus on form quality - you had more bad reps than good ones")
        }

        // Range of motion recommendations
        if (rom.consistency < 70f) {
            recommendations.add("Work on consistency - your depth varies significantly between reps")
        }

        // Symmetry recommendations
        if (symmetry.overallSymmetry < 80f) {
            recommendations.add("Check for muscle imbalances - you're favoring one side")
        }

        // Tempo recommendations
        if (tempo.tempoConsistency < 70f) {
            recommendations.add("Try to maintain a consistent tempo between reps")
        }

        // Exercise-specific recommendations
        when (exerciseMetrics) {
            is ExerciseSpecificMetrics.SquatMetrics -> {
                if (exerciseMetrics.kneeTracking.kneeCavePercentage > 30f) {
                    recommendations.add("Focus on pushing knees out - ${exerciseMetrics.kneeTracking.kneeCavePercentage.toInt()}% of reps showed knee cave")
                }
                if (exerciseMetrics.averageDepth > 110f) {
                    recommendations.add("Work on hip mobility to achieve deeper squats")
                }
                if (exerciseMetrics.torsoAngle.excessiveLean) {
                    recommendations.add("Maintain more upright torso - excessive forward lean detected")
                }
            }
            is ExerciseSpecificMetrics.DeadliftMetrics -> {
                if (exerciseMetrics.hipHingeQuality < 70f) {
                    recommendations.add("Practice hip hinge pattern - initiate movement with hips, not back")
                }
                if (exerciseMetrics.backStraightness.lowerBackRounding > 20f) {
                    recommendations.add("Maintain neutral spine - lower back rounding detected")
                }
                if (exerciseMetrics.lockoutCompletion < 90f) {
                    recommendations.add("Drive hips through fully at the top for complete lockout")
                }
            }
            is ExerciseSpecificMetrics.BenchPressMetrics -> {
                if (exerciseMetrics.elbowAngle.bottomAngle > 100f) {
                    recommendations.add("Lower the bar deeper - aim for 90° elbow angle at bottom")
                }
                if (exerciseMetrics.elbowAngle.tucking < 70f) {
                    recommendations.add("Tuck elbows more - aim for 45° angle from body")
                }
            }
            null -> {
                // No exercise-specific metrics available
            }
        }

        // Positive reinforcement
        if (session.goodReps > session.badReps && rom.consistency > 80f) {
            recommendations.add("Great workout! Your form and consistency are excellent")
        }

        return recommendations.ifEmpty {
            listOf("Keep up the good work!")
        }
    }
}
