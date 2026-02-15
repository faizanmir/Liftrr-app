package org.liftrr.domain.analytics

import org.liftrr.domain.workout.RepData
import org.liftrr.ml.ExerciseType
import org.liftrr.ml.PoseAnalyzer
import org.liftrr.ml.PoseDetectionResult
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * High-Performance Workout Analyzer
 * Processes recorded session data to generate technical insights and coaching.
 */
object WorkoutAnalyzer {

    fun analyzeSession(session: WorkoutSession): WorkoutReport {
        val repAnalyses = analyzeAllReps(session)
        val rangeOfMotion = analyzeRangeOfMotion(repAnalyses)
        val tempo = analyzeTempo(session, repAnalyses)
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

    private fun analyzeAllReps(session: WorkoutSession): List<RepAnalysis> {
        return session.reps.map { rep ->
            val repFrames = session.poseFrames.filter { it.repNumber == rep.repNumber }
            analyzeRep(rep, repFrames, session.exerciseType)
        }
    }

    private fun analyzeRep(rep: RepData, frames: List<PoseFrame>, type: ExerciseType): RepAnalysis {
        val successFrames = frames.mapNotNull { it.poseResult as? PoseDetectionResult.Success }

        // Fixed: Use direct instantiation instead of non-existent .empty()
        if (successFrames.isEmpty()) {
            return RepAnalysis(
                repNumber = rep.repNumber,
                isGoodForm = rep.isGoodForm,
                quality = rep.poseQuality,
                depth = null,
                tempo = null,
                peakFrameIndex = null,
                formIssues = listOf("Insufficient pose data")
            )
        }

        val peakIdx = findActualPeakFrame(successFrames, type)
        return RepAnalysis(
            repNumber = rep.repNumber,
            isGoodForm = rep.isGoodForm,
            quality = rep.poseQuality,
            depth = calculateRepDepth(successFrames, type),
            tempo = calculateAccurateTempo(frames, peakIdx),
            peakFrameIndex = peakIdx,
            formIssues = identifyFormIssues(successFrames, type)
        )
    }

    // --- Biomechanical Calculation Core ---

    private fun findActualPeakFrame(
        frames: List<PoseDetectionResult.Success>, type: ExerciseType
    ): Int {
        val angles = frames.map { pose ->
            getBilateralAngle(pose, getPrimaryJoints(type)) ?: 180f
        }
        // Deadlift peak is max extension (top); others are min angle (bottom)
        return if (type == ExerciseType.DEADLIFT) angles.indexOf(angles.maxOrNull()) else angles.indexOf(
            angles.minOrNull()
        )
    }

    private fun calculateAccurateTempo(frames: List<PoseFrame>, peakIndex: Int): RepTempo? {
        if (frames.size < 2) return null
        val start = frames.first().timestamp
        val peak = frames.getOrNull(peakIndex)?.timestamp ?: start
        val end = frames.last().timestamp
        return RepTempo(
            totalMs = end - start, eccentricMs = peak - start, concentricMs = end - peak
        )
    }

    private fun calculateRepDepth(
        frames: List<PoseDetectionResult.Success>, type: ExerciseType
    ): Float? {
        val angles = frames.mapNotNull { getBilateralAngle(it, getPrimaryJoints(type)) }
        return if (type == ExerciseType.DEADLIFT) angles.maxOrNull() else angles.minOrNull()
    }

    private fun identifyFormIssues(
        frames: List<PoseDetectionResult.Success>, type: ExerciseType
    ): List<String> {
        val issues = mutableSetOf<String>()
        frames.forEach { pose ->
            when (type) {
                ExerciseType.SQUAT -> {
                    if (calculateValgusRatio(pose) < 0.88f) issues.add("Knee Cave")
                    calculateForwardLean(pose)?.let { if (it > 45f) issues.add("Torso Collapse") }
                }

                ExerciseType.DEADLIFT -> {
                    if (calculateBackAlignment(pose) < 0.82f) issues.add("Rounded Back")
                }

                ExerciseType.BENCH_PRESS -> {
                    calculateElbowFlare(pose)?.let { if (it > 80f) issues.add("Excessive Flare") }
                }
            }
        }
        return issues.toList()
    }

    // --- Metric Aggregators ---

    // Cleaned up version without unused session
    private fun analyzeRangeOfMotion(analyses: List<RepAnalysis>): RangeOfMotionAnalysis {
        val depths = analyses.mapNotNull { it.depth }
        if (depths.isEmpty()) return RangeOfMotionAnalysis(0f, 0f, 0f, 0f)

        val avg = depths.average().toFloat()
        return RangeOfMotionAnalysis(
            averageDepth = avg,
            minDepth = depths.minOrNull() ?: 0f,
            maxDepth = depths.maxOrNull() ?: 0f,
            consistency = calculateConsistency(depths)
        )
    }

    private fun analyzeTempo(session: WorkoutSession, analyses: List<RepAnalysis>): TempoAnalysis {
        val repDurations = analyses.mapNotNull { it.tempo?.totalMs }
        val avgDuration = if (repDurations.isNotEmpty()) repDurations.average().toLong() else 0L

        // Using session to calculate rest between reps
        val restPeriods = mutableListOf<Long>()
        for (i in 0 until session.reps.size - 1) {
            val currentRepEnd = session.reps[i].timestamp
            val nextRepStart = session.reps[i + 1].timestamp

            // This assumes timestamp is the end of the rep.
            // If it's the start, you'd subtract currentDuration.
            val rest = nextRepStart - currentRepEnd
            if (rest > 0) restPeriods.add(rest)
        }

        val avgRest = if (restPeriods.isNotEmpty()) restPeriods.average().toLong() else 0L

        return TempoAnalysis(
            averageRepDurationMs = avgDuration,
            averageRestBetweenRepsMs = avgRest,
            tempoConsistency = calculateConsistency(repDurations.map { it.toFloat() })
        )
    }

    private fun analyzeSymmetry(session: WorkoutSession): SymmetryAnalysis {
        val frames = session.poseFrames.mapNotNull { it.poseResult as? PoseDetectionResult.Success }
        val scores = frames.mapNotNull { calculateSymmetryScore(it, session.exerciseType) }
        val avg = if (scores.isNotEmpty()) scores.average().toFloat() else 100f
        return SymmetryAnalysis(
            avg, 100f - avg, if (avg < 85f) listOf("Imbalanced drive") else emptyList()
        )
    }

    private fun analyzeFormConsistency(session: WorkoutSession): FormConsistencyAnalysis {
        val qualities = session.reps.map { it.poseQuality }
        if (qualities.isNotEmpty()) qualities.average().toFloat() else 0f
        val firstHalf = qualities.take(qualities.size / 2).average()
        val secondHalf = qualities.drop(qualities.size / 2).average()
        val trend = when {
            secondHalf > firstHalf + 0.1 -> "Improving"
            secondHalf < firstHalf - 0.1 -> "Declining"
            else -> "Stable"
        }
        return FormConsistencyAnalysis(calculateConsistency(qualities) * 100f, trend)
    }

    // --- Exercise Specific Implementation ---

    private fun analyzeExerciseSpecificMetrics(session: WorkoutSession): ExerciseSpecificMetrics? {
        val frames = session.poseFrames.mapNotNull { it.poseResult as? PoseDetectionResult.Success }
        if (frames.isEmpty()) return null
        return when (session.exerciseType) {
            ExerciseType.SQUAT -> analyzeSquatMetrics(frames)
            ExerciseType.DEADLIFT -> analyzeDeadliftMetrics(frames)
            ExerciseType.BENCH_PRESS -> analyzeBenchMetrics(frames)
        }
    }

    private fun analyzeSquatMetrics(frames: List<PoseDetectionResult.Success>): ExerciseSpecificMetrics.SquatMetrics {
        val depths = frames.mapNotNull { getBilateralAngle(it, 23, 25, 27) }
        val leans = frames.mapNotNull { calculateForwardLean(it) }
        val valgus = frames.map { calculateValgusRatio(it) }

        val minDepth = depths.minOrNull() ?: 180f
        val cavePct = (valgus.count { it < 0.88f }.toFloat() / valgus.size.coerceAtLeast(1)) * 100f

        return ExerciseSpecificMetrics.SquatMetrics(
            averageDepth = depths.average().toFloat(),
            depthConsistency = calculateConsistency(depths),
            kneeTracking = KneeTrackingAnalysis(
                valgus.average().toFloat() * 100f, cavePct, 85f, emptyList()
            ),
            hipMobility = calculateHipMobilityScore(minDepth),
            torsoAngle = TorsoAngleAnalysis(
                leans.average().toFloat(), calculateConsistency(leans), leans.average() > 45f
            ),
            balance = BalanceAnalysis(85f, 80f, 90f, emptyList())
        )
    }

    private fun analyzeDeadliftMetrics(frames: List<PoseDetectionResult.Success>): ExerciseSpecificMetrics.DeadliftMetrics {
        val backs = frames.map { calculateBackAlignment(it) }
        val lockouts = frames.mapNotNull { getBilateralAngle(it, 11, 23, 25) }
        val worstBack = backs.minOrNull() ?: 1.0f

        return ExerciseSpecificMetrics.DeadliftMetrics(
            hipHingeQuality = 85f,
            backStraightness = BackAnalysis(
                worstBack * 100f, if (worstBack < 0.82f) 20f else 0f, 0f, emptyList()
            ),
            lockoutCompletion = ((lockouts.maxOrNull() ?: 0f) / 170f * 100f).coerceIn(0f, 100f),
            barPath = null,
            startingPosition = StartPositionAnalysis(85f, 85f, 85f, emptyList())
        )
    }

    private fun analyzeBenchMetrics(frames: List<PoseDetectionResult.Success>): ExerciseSpecificMetrics.BenchPressMetrics {
        val flares = frames.mapNotNull { calculateElbowFlare(it) }
        val elbows = frames.mapNotNull { getBilateralAngle(it, 11, 13, 15) }

        return ExerciseSpecificMetrics.BenchPressMetrics(
            barPath = null,
            elbowAngle = ElbowAngleAnalysis(
                elbows.minOrNull() ?: 0f,
                elbows.maxOrNull() ?: 180f,
                calculateConsistency(elbows),
                100f - flares.average().toFloat()
            ),
            shoulderPosition = ShoulderAnalysis(85f, 80f, 90f, emptyList()),
            touchPointConsistency = 88f,
            archMaintenance = 85f
        )
    }

    // --- Math Utilities & Guards ---

    private fun getBilateralAngle(
        pose: PoseDetectionResult.Success, joints: Triple<Int, Int, Int>
    ) = getBilateralAngle(pose, joints.first, joints.second, joints.third)

    private fun getBilateralAngle(
        pose: PoseDetectionResult.Success, a: Int, b: Int, c: Int
    ): Float? {
        val lA = pose.getLandmark(a)
        val lB = pose.getLandmark(b)
        val lC = pose.getLandmark(c)
        val rA = pose.getLandmark(a + 1)
        val rB = pose.getLandmark(b + 1)
        val rC = pose.getLandmark(c + 1)

        val left = if (lA != null && lB != null && lC != null) PoseAnalyzer.calculateAngle(
            lA, lB, lC
        ) else null
        val right = if (rA != null && rB != null && rC != null) PoseAnalyzer.calculateAngle(
            rA, rB, rC
        ) else null

        return when {
            left != null && right != null -> (left + right) / 2f
            else -> left ?: right
        }
    }

    private fun calculateForwardLean(pose: PoseDetectionResult.Success): Float? {
        val s = pose.getLandmark(11) ?: return null
        val h = pose.getLandmark(23) ?: return null
        return Math.toDegrees(atan2(abs(s.x() - h.x()).toDouble(), abs(s.y() - h.y()).toDouble()))
            .toFloat()
    }

    private fun calculateConsistency(values: List<Float>): Float {
        if (values.size < 2) return 100f
        val avg = values.average().toFloat()
        val stdDev = sqrt(values.map { (it - avg).toDouble().pow(2.0) }.average()).toFloat()
        return (100f - (stdDev / avg.coerceAtLeast(1f) * 100f)).coerceIn(0f, 100f)
    }

    private fun calculateHipMobilityScore(minAngle: Float): Float = when {
        minAngle <= 95f -> 100f
        minAngle <= 110f -> 85f
        else -> 60f
    }

    private fun calculateValgusRatio(pose: PoseDetectionResult.Success): Float {
        val lK = pose.getLandmark(25)
        val rK = pose.getLandmark(26)
        val lA = pose.getLandmark(27)
        val rA = pose.getLandmark(28)
        if (lK == null || rK == null || lA == null || rA == null) return 1.0f
        return abs(lK.x() - rK.x()) / abs(lA.x() - rA.x()).coerceAtLeast(0.01f)
    }

    private fun calculateBackAlignment(pose: PoseDetectionResult.Success): Float {
        val s = pose.getLandmark(11)
        val h = pose.getLandmark(23)
        val k = pose.getLandmark(25)
        if (s == null || h == null || k == null) return 1.0f
        return abs(s.y() - h.y()) / abs(h.y() - k.y()).coerceAtLeast(0.01f)
    }

    private fun calculateElbowFlare(pose: PoseDetectionResult.Success): Float? {
        val lH = pose.getLandmark(23)
        val lS = pose.getLandmark(11)
        val lE = pose.getLandmark(13)
        val rH = pose.getLandmark(24)
        val rS = pose.getLandmark(12)
        val rE = pose.getLandmark(14)

        val left = if (lH != null && lS != null && lE != null) PoseAnalyzer.calculateAngle(
            lH, lS, lE
        ) else null
        val right = if (rH != null && rS != null && rE != null) PoseAnalyzer.calculateAngle(
            rH, rS, rE
        ) else null

        return when {
            left != null && right != null -> (left + right) / 2f
            else -> left ?: right
        }
    }

    private fun calculateSymmetryScore(
        pose: PoseDetectionResult.Success, type: ExerciseType
    ): Float? {
        val joints = getPrimaryJoints(type)
        val l = getBilateralAngle(
            pose, joints.first, joints.second, joints.third
        ) // Simplified using our own helper
        val r = getBilateralAngle(pose, joints.first + 1, joints.second + 1, joints.third + 1)
        if (l == null || r == null) return null
        return (100f - (abs(l - r) / l.coerceAtLeast(r).coerceAtLeast(1f) * 100f)).coerceIn(
            0f, 100f
        )
    }

    private fun getPrimaryJoints(type: ExerciseType) = when (type) {
        ExerciseType.SQUAT -> Triple(23, 25, 27)
        ExerciseType.DEADLIFT -> Triple(11, 23, 25)
        ExerciseType.BENCH_PRESS -> Triple(11, 13, 15)
    }

    private fun generateRecommendations(
        session: WorkoutSession,
        reps: List<RepAnalysis>,
        rom: RangeOfMotionAnalysis,
        tempo: TempoAnalysis,
        symmetry: SymmetryAnalysis,
        metrics: ExerciseSpecificMetrics?
    ): List<String> {
        val recs = mutableListOf<String>()

        // 1. SAFETY CRITICALS (High Priority)
        when (metrics) {
            is ExerciseSpecificMetrics.DeadliftMetrics -> {
                if (metrics.backStraightness.spineNeutral < 80f) {
                    recs.add("CRITICAL: Spine rounding detected. Lower the weight and focus on a neutral lower back.")
                }
                if (metrics.lockoutCompletion < 90f) {
                    recs.add("Drive your hips through fully at the top to complete the lockout.")
                }
            }

            is ExerciseSpecificMetrics.SquatMetrics -> {
                if (metrics.kneeTracking.kneeCavePercentage > 25f) {
                    recs.add("Knees caving: Think about 'screwing' your feet into the floor to drive your knees out.")
                }
                if (metrics.torsoAngle.averageForwardLean > 45f) {
                    recs.add("You're leaning too far forward. Keep your chest up and lead with your heart.")
                }
            }

            is ExerciseSpecificMetrics.BenchPressMetrics -> {
                if (metrics.elbowAngle.tucking < 60f) {
                    recs.add("Shoulder Safety: Tuck your elbows slightly more toward your ribs to avoid shoulder strain.")
                }
            }

            null -> {}
        }

        // 2. CONSISTENCY & ROM (Technical Proficiency)
        if (rom.averageDepth > 115f && session.exerciseType == ExerciseType.SQUAT) {
            recs.add("Work on your mobility; your squats are currently a bit shallow.")
        } else if (rom.consistency < 75f) {
            recs.add("Consistency is key. Focus on hitting the same depth/turnaround point on every rep.")
        }

        // 3. TEMPO & CONTROL (Rhythm)
        val avgEccentric = reps.mapNotNull { it.tempo?.eccentricMs }.average()
        val avgConcentric = reps.mapNotNull { it.tempo?.concentricMs }.average()

        if (avgEccentric < 800) {
            recs.add("Slow down your descent. Aim for a 2-second 'eccentric' phase for better muscle growth.")
        }

        // Check for "grinding" reps (slow concentric)
        if (avgConcentric > 2500 && session.goodReps > 0) {
            recs.add("Your rep speed is slowing down significantly. You're approaching technical failure.")
        }

        // 4. SYMMETRY (Balance)
        if (symmetry.overallSymmetry < 82f) {
            recs.add("You're favoring one side. Focus on an even weight distribution across both feet.")
        }

        // 5. REINFORCEMENT
        if (recs.isEmpty() && session.goodReps.toFloat() / session.totalReps > 0.9f) {
            recs.add("Masterful performance! Your technique and consistency are elite level.")
        }

        return recs.ifEmpty { listOf("Solid session. Keep maintaining this level of control.") }
    }
}