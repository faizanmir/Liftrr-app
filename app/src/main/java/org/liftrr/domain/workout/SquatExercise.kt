package org.liftrr.domain.workout

import org.liftrr.ml.PoseDetectionResult
import kotlin.math.abs

class SquatExercise : Exercise {

    enum class State { STANDING, DESCENT, BOTTOM, ASCENT }

    private var currentState = State.STANDING
    private var lastRepTime = 0L
    private var frameStabilityCount = 0

    private val kneeSmoother = AngleSmoother(5)
    private val hipSmoother = AngleSmoother(5)

    // Rep Metrics
    private var repMinKneeAngle = 180f
    private var repMaxForwardLean = 0f
    private var repMaxValgusRatio = 0f
    private var lastFormScore = 100f

    companion object {
        private const val KNEE_BOTTOM_THRESHOLD = 105f // Parallel is roughly 90-105°
        private const val KNEE_LOCKOUT_THRESHOLD = 165f
        private const val FORWARD_LEAN_LIMIT = 45f    // Degrees of torso tilt
        private const val VALGUS_THRESHOLD = 0.88f    // Knee width / Ankle width

        private const val MIN_REP_DURATION = 1100L
        private const val STABILITY_REQUIRED = 3
    }

    override fun updateRepCount(pose: PoseDetectionResult.Success): Boolean {
        // 1. Calculate Core Angles (Bilateral averages help with camera angle noise)
        val kneeAngle = getBilateralAngle(pose, 23, 25, 27) // Hip, Knee, Ankle
        val hipAngle = getBilateralAngle(pose, 11, 23, 25)  // Shoulder, Hip, Knee

        if (kneeAngle == null || hipAngle == null) return false

        val smoothedKnee = kneeSmoother.add(kneeAngle)
        val smoothedHip = hipSmoother.add(hipAngle)

        // 2. Track Form during the rep
        val forwardLean = abs(180f - smoothedHip)
        val valgusRatio = calculateValgusRatio(pose)

        if (smoothedKnee < repMinKneeAngle) repMinKneeAngle = smoothedKnee
        if (forwardLean > repMaxForwardLean) repMaxForwardLean = forwardLean
        if (valgusRatio < repMaxValgusRatio || repMaxValgusRatio == 0f) repMaxValgusRatio = valgusRatio

        return handleStateMachine(smoothedKnee)
    }

    private fun handleStateMachine(knee: Float): Boolean {
        return when (currentState) {
            State.STANDING -> {
                if (knee < KNEE_LOCKOUT_THRESHOLD - 10f) currentState = State.DESCENT
                false
            }
            State.DESCENT -> {
                if (knee < KNEE_BOTTOM_THRESHOLD) {
                    frameStabilityCount++
                    if (frameStabilityCount >= STABILITY_REQUIRED) {
                        currentState = State.BOTTOM
                        frameStabilityCount = 0
                    }
                }
                false
            }
            State.BOTTOM -> {
                if (knee > KNEE_BOTTOM_THRESHOLD + 15f) currentState = State.ASCENT
                false
            }
            State.ASCENT -> {
                if (knee > KNEE_LOCKOUT_THRESHOLD) {
                    val now = System.currentTimeMillis()
                    if (now - lastRepTime > MIN_REP_DURATION) {
                        completeRep(now)
                        return true
                    }
                }
                false
            }
        }
    }

    private fun calculateValgusRatio(pose: PoseDetectionResult.Success): Float {
        val lKnee = pose.getLandmark(25); val rKnee = pose.getLandmark(26)
        val lAnkle = pose.getLandmark(27); val rAnkle = pose.getLandmark(28)

        if (lKnee == null || rKnee == null || lAnkle == null || rAnkle == null) return 1.0f

        val kneeWidth = abs(lKnee.x() - rKnee.x())
        val ankleWidth = abs(lAnkle.x() - rAnkle.x())

        return if (ankleWidth > 0.05f) kneeWidth / ankleWidth else 1.0f
    }

    private fun calculateFormScore(): Float {
        var score = 100f

        // Penalty: Depth (Range of Motion)
        if (repMinKneeAngle > 110f) score -= 35f

        // Penalty: Knee Valgus (Safety)
        if (repMaxValgusRatio < VALGUS_THRESHOLD) score -= 25f

        // Penalty: Excessive Forward Lean (Good Morning Squat)
        if (repMaxForwardLean > FORWARD_LEAN_LIMIT) score -= 20f

        return score.coerceIn(0f, 100f)
    }

    override fun analyzeFeedback(pose: PoseDetectionResult.Success): String {
        val knee = kneeSmoother.lastValue
        val hip = hipSmoother.lastValue
        val valgus = calculateValgusRatio(pose)

        return when {
            valgus < VALGUS_THRESHOLD -> "Push your knees out!"
            abs(180f - hip) > FORWARD_LEAN_LIMIT -> "Keep your chest up!"
            knee > KNEE_BOTTOM_THRESHOLD && currentState == State.DESCENT -> "Go a bit lower"
            knee > KNEE_LOCKOUT_THRESHOLD -> "Good stand! Start next rep."
            else -> "Drive through your mid-foot"
        }
    }

    override fun detectMovementPhase(pose: PoseDetectionResult.Success): MovementPhase {
        return when (currentState) {
            State.STANDING -> MovementPhase.LOCKOUT
            State.DESCENT -> MovementPhase.DESCENT
            State.BOTTOM -> MovementPhase.BOTTOM
            State.ASCENT -> MovementPhase.ASCENT
        }
    }

    override fun getFormDiagnostics(pose: PoseDetectionResult.Success): List<FormDiagnostic> {
        val knee = kneeSmoother.lastValue
        val valgus = calculateValgusRatio(pose)

        return listOf(
            FormDiagnostic(
                issue = if (knee < KNEE_BOTTOM_THRESHOLD + 5f) "Good Depth" else "Shallow Depth",
                angle = "Knee Angle",
                measured = knee,
                expected = "< 105°",
                severity = if (knee < KNEE_BOTTOM_THRESHOLD + 5f) FormIssueSeverity.GOOD else FormIssueSeverity.MODERATE
            ),
            FormDiagnostic(
                issue = if (valgus < VALGUS_THRESHOLD) "Knees Caving" else "Good Knee Tracking",
                angle = "Valgus Ratio",
                measured = valgus * 100,
                expected = "> 88%",
                severity = if (valgus < VALGUS_THRESHOLD) FormIssueSeverity.CRITICAL else FormIssueSeverity.GOOD
            )
        )
    }

    private fun completeRep(time: Long) {
        lastFormScore = calculateFormScore()
        lastRepTime = time
        resetRepMetrics()
        currentState = State.STANDING
    }

    private fun resetRepMetrics() {
        repMinKneeAngle = 180f
        repMaxForwardLean = 0f
        repMaxValgusRatio = 1.0f
    }

    override fun reset() {
        currentState = State.STANDING
        lastRepTime = 0L
        resetRepMetrics()
        kneeSmoother.reset()
        hipSmoother.reset()
    }

    private fun getBilateralAngle(pose: PoseDetectionResult.Success, a: Int, b: Int, c: Int): Float? {
        val left = AngleCalculator.calculateAngle(pose.getLandmark(a), pose.getLandmark(b), pose.getLandmark(c))
        val right = AngleCalculator.calculateAngle(pose.getLandmark(a + 1), pose.getLandmark(b + 1), pose.getLandmark(c + 1))
        return if (left != null && right != null) (left + right) / 2f else left ?: right
    }

    override fun formScore(): Float = lastFormScore
    override fun hadGoodForm(): Boolean = lastFormScore >= 70f
}