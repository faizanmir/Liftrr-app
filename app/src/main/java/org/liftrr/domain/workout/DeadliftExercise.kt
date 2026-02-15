package org.liftrr.domain.workout

import org.liftrr.ml.PoseDetectionResult
import kotlin.math.abs

class DeadliftExercise : Exercise {

    enum class State { SETUP, ASCENDING, LOCKOUT, DESCENDING }

    // --- State & Timing ---
    private var currentState = State.SETUP
    private var lastRepTime = 0L
    private var frameStabilityCount = 0

    // --- Smoothing Filters ---
    private val hipSmoother = AngleSmoother(5)
    private val kneeSmoother = AngleSmoother(5)

    // --- Metrics for Score Calculation ---
    private var repMinKneeAngle = 180f
    private var repMaxShoulderDrift = 0f
    private var worstBackRatio = 1.0f
    private var lastFormScore = 100f

    companion object {
        // Biomechanical Logic Thresholds
        private const val HIP_START_THRESHOLD = 125f  // Hinge depth to start
        private const val HIP_LOCKOUT_TARGET = 168f   // Full extension target
        private const val KNEE_SQUAT_MIN = 95f        // Angle < 95 indicates "squatting" the lift
        private const val DRIFT_MAX_X = 0.10f         // Horizontal drift allowance
        private const val BACK_SAFETY_RATIO = 0.82f   // Torso height vs Thigh height

        private const val MIN_REP_DURATION_MS = 1100L
        private const val STABILITY_REQUIRED = 3
    }

    override fun updateRepCount(pose: PoseDetectionResult.Success): Boolean {
        // 1. Calculate and Smooth Key Angles
        val hipAngle = getBilateralAngle(pose, 11, 23, 25) // Shoulder, Hip, Knee
        val kneeAngle = getBilateralAngle(pose, 23, 25, 27) // Hip, Knee, Ankle

        if (hipAngle == null || kneeAngle == null) return false

        val smoothedHip = hipSmoother.add(hipAngle)
        val smoothedKnee = kneeSmoother.add(kneeAngle)

        // 2. Spine Neutrality (Normalized to camera distance)
        val currentBackRatio = calculateNormalizedSpine(pose)
        if (currentBackRatio < worstBackRatio) worstBackRatio = currentBackRatio

        // 3. Horizontal Bar Path Drift (Shoulder X vs Ankle X)
        val avgShoulderX = (pose.getLandmark(11)!!.x() + pose.getLandmark(12)!!.x()) / 2f
        val avgAnkleX = (pose.getLandmark(27)!!.x() + pose.getLandmark(28)!!.x()) / 2f
        val currentDrift = abs(avgShoulderX - avgAnkleX)

        // Track technical flaws only during the active ascent
        if (currentState == State.ASCENDING) {
            if (smoothedKnee < repMinKneeAngle) repMinKneeAngle = smoothedKnee
            if (currentDrift > repMaxShoulderDrift) repMaxShoulderDrift = currentDrift
        }

        return handleStateMachine(smoothedHip)
    }

    private fun handleStateMachine(hip: Float): Boolean {
        return when (currentState) {
            State.SETUP -> {
                if (hip < HIP_START_THRESHOLD) {
                    frameStabilityCount++
                    if (frameStabilityCount >= STABILITY_REQUIRED) {
                        currentState = State.ASCENDING
                        frameStabilityCount = 0
                    }
                }
                false
            }
            State.ASCENDING -> {
                if (hip >= HIP_LOCKOUT_TARGET) currentState = State.LOCKOUT
                false
            }
            State.LOCKOUT -> {
                // Confirm rep when hips drop significantly below lockout (start of descent)
                if (hip < HIP_LOCKOUT_TARGET - 6f) {
                    val now = System.currentTimeMillis()
                    if (now - lastRepTime > MIN_REP_DURATION_MS) {
                        completeRep(now)
                        return true
                    }
                    currentState = State.DESCENDING
                }
                false
            }
            State.DESCENDING -> {
                if (hip < HIP_START_THRESHOLD) currentState = State.SETUP
                false
            }
        }
    }

    private fun calculateNormalizedSpine(pose: PoseDetectionResult.Success): Float {
        val s = pose.getLandmark(11) ?: return 1.0f
        val h = pose.getLandmark(23) ?: return 1.0f
        val k = pose.getLandmark(25) ?: return 1.0f

        val torsoVerticalHeight = abs(s.y() - h.y())
        val thighVerticalHeight = abs(h.y() - k.y())

        if (thighVerticalHeight < 0.01f) return 1.0f
        return torsoVerticalHeight / thighVerticalHeight
    }

    private fun calculateFormScore(): Float {
        var score = 100f

        // Priority 1: Back Rounding (Safety Hazard)
        if (worstBackRatio < BACK_SAFETY_RATIO) {
            val backPenalty = ((BACK_SAFETY_RATIO - worstBackRatio) * 300f).coerceAtMost(50f)
            score -= backPenalty
        }

        // Priority 2: Hip Hinge Quality (Preventing Squatting)
        if (repMinKneeAngle < KNEE_SQUAT_MIN) score -= 20f

        // Priority 3: Bar Path Verticality
        if (repMaxShoulderDrift > DRIFT_MAX_X) score -= 15f

        return score.coerceIn(0f, 100f)
    }

    override fun analyzeFeedback(pose: PoseDetectionResult.Success): String {
        val currentBack = calculateNormalizedSpine(pose)
        val hip = hipSmoother.lastValue

        return when {
            currentBack < BACK_SAFETY_RATIO -> "Flatten your back! Chest up."
            repMaxShoulderDrift > DRIFT_MAX_X -> "Keep the bar closer to your body."
            repMinKneeAngle < KNEE_SQUAT_MIN -> "Hips higher! Don't squat the weight."
            hip >= HIP_LOCKOUT_TARGET -> "Perfect lockout! Now lower slowly."
            else -> "Drive through your heels!"
        }
    }

    override fun detectMovementPhase(pose: PoseDetectionResult.Success): MovementPhase {
        val hip = hipSmoother.lastValue
        return when {
            hip >= HIP_LOCKOUT_TARGET -> MovementPhase.LOCKOUT
            currentState == State.ASCENDING -> MovementPhase.ASCENT
            currentState == State.DESCENDING || hip < HIP_START_THRESHOLD -> MovementPhase.BOTTOM
            else -> MovementPhase.SETUP
        }
    }

    override fun getFormDiagnostics(pose: PoseDetectionResult.Success): List<FormDiagnostic> {
        val backRatio = calculateNormalizedSpine(pose)
        val hip = hipSmoother.lastValue

        return listOf(
            FormDiagnostic(
                issue = if (backRatio < BACK_SAFETY_RATIO) "Rounded Spine" else "Neutral Spine",
                angle = "Torso Ratio",
                measured = backRatio * 100,
                expected = "82%+",
                severity = if (backRatio < BACK_SAFETY_RATIO) FormIssueSeverity.CRITICAL else FormIssueSeverity.GOOD
            ),
            FormDiagnostic(
                issue = "Hip Lockout",
                angle = "Hip Angle",
                measured = hip,
                expected = "${HIP_LOCKOUT_TARGET.toInt()}°",
                severity = if (hip >= HIP_LOCKOUT_TARGET) FormIssueSeverity.GOOD else FormIssueSeverity.MODERATE
            )
        )
    }

    private fun completeRep(time: Long) {
        lastFormScore = calculateFormScore()
        lastRepTime = time
        resetRepMetrics()
        currentState = State.SETUP
    }

    private fun resetRepMetrics() {
        repMinKneeAngle = 180f
        repMaxShoulderDrift = 0f
        worstBackRatio = 1.0f
    }

    override fun reset() {
        currentState = State.SETUP
        lastRepTime = 0L
        resetRepMetrics()
        hipSmoother.reset()
        kneeSmoother.reset()
    }

    private fun getBilateralAngle(pose: PoseDetectionResult.Success, a: Int, b: Int, c: Int): Float? {
        val left = AngleCalculator.calculateAngle(pose.getLandmark(a), pose.getLandmark(b), pose.getLandmark(c))
        val right = AngleCalculator.calculateAngle(pose.getLandmark(a + 1), pose.getLandmark(b + 1), pose.getLandmark(c + 1))
        return if (left != null && right != null) (left + right) / 2f else left ?: right
    }

    override fun formScore(): Float = lastFormScore
    override fun hadGoodForm(): Boolean = lastFormScore >= 70f
}