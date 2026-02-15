package org.liftrr.domain.workout

import org.liftrr.ml.PoseDetectionResult

class BenchPressExercise : Exercise {

    enum class State { START, DESCENT, BOTTOM, ASCENT }

    private var currentState = State.START
    private var lastRepTime = 0L
    private var frameStabilityCount = 0

    private val elbowSmoother = AngleSmoother(5)

    // Rep Tracking Metrics
    private var repMinElbowAngle = 180f
    private var repMaxFlareAngle = 0f
    private var repMaxLockoutAngle = 0f
    private var lastFormScore = 100f

    companion object {
        // Biomechanical Thresholds
        private const val ELBOW_BOTTOM_LIMIT = 85f   // Bar near chest (90 degrees or less)
        private const val ELBOW_LOCKOUT_TARGET = 160f
        private const val FLARE_LIMIT_HIGH = 80f      // Dangerous shoulder stress (T-pose)
        private const val FLARE_LIMIT_LOW = 30f       // Too tucked

        private const val MIN_REP_DURATION = 1100L
        private const val STABILITY_FRAMES = 3
    }

    override fun updateRepCount(pose: PoseDetectionResult.Success): Boolean {
        // 1. Core Angle Calculation
        val elbowAngle =
            getBilateralAngle(pose, 11, 13, 15) ?: return false // Shoulder, Elbow, Wrist

        val smoothedElbow = elbowSmoother.add(elbowAngle)

        // 2. Calculate Elbow Flare (Angle of humerus relative to torso)
        val flareAngle = calculateElbowFlare(pose)

        // Track metrics during the rep journey
        if (smoothedElbow < repMinElbowAngle) repMinElbowAngle = smoothedElbow
        if (flareAngle > repMaxFlareAngle) repMaxFlareAngle = flareAngle
        if (smoothedElbow > repMaxLockoutAngle) repMaxLockoutAngle = smoothedElbow

        return handleStateMachine(smoothedElbow)
    }

    private fun handleStateMachine(elbow: Float): Boolean {
        return when (currentState) {
            State.START -> {
                if (elbow < ELBOW_LOCKOUT_TARGET - 10f) currentState = State.DESCENT
                false
            }
            State.DESCENT -> {
                if (elbow < ELBOW_BOTTOM_LIMIT) {
                    frameStabilityCount++
                    if (frameStabilityCount >= STABILITY_FRAMES) {
                        currentState = State.BOTTOM
                        frameStabilityCount = 0
                    }
                }
                false
            }
            State.BOTTOM -> {
                // Hysteresis: Require significant movement before switching to ascent
                if (elbow > ELBOW_BOTTOM_LIMIT + 15f) currentState = State.ASCENT
                false
            }
            State.ASCENT -> {
                if (elbow > ELBOW_LOCKOUT_TARGET) {
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

    private fun calculateElbowFlare(pose: PoseDetectionResult.Success): Float {
        // Measures angle between Torso (Hip-Shoulder) and Upper Arm (Shoulder-Elbow)
        val leftFlare = AngleCalculator.calculateAngle(pose.getLandmark(23), pose.getLandmark(11), pose.getLandmark(13))
        val rightFlare = AngleCalculator.calculateAngle(pose.getLandmark(24), pose.getLandmark(12), pose.getLandmark(14))
        return if (leftFlare != null && rightFlare != null) (leftFlare + rightFlare) / 2f else leftFlare ?: rightFlare ?: 0f
    }

    private fun calculateFormScore(): Float {
        var score = 100f

        // Penalty 1: Depth / Range of Motion
        if (repMinElbowAngle > 95f) score -= 35f

        // Penalty 2: Excessive Flare (Shoulder Safety)
        if (repMaxFlareAngle > FLARE_LIMIT_HIGH) score -= 25f

        // Penalty 3: Soft Lockout
        if (repMaxLockoutAngle < ELBOW_LOCKOUT_TARGET) score -= 15f

        return score.coerceIn(0f, 100f)
    }

    override fun analyzeFeedback(pose: PoseDetectionResult.Success): String {
        val elbow = elbowSmoother.lastValue
        val flare = calculateElbowFlare(pose)

        return when {
            flare > FLARE_LIMIT_HIGH -> "Tuck elbows in for shoulder safety!"
            elbow > ELBOW_LOCKOUT_TARGET -> "Good lockout!"
            currentState == State.DESCENT && elbow > 105f -> "Bring bar all the way to chest"
            currentState == State.ASCENT -> "Drive the bar up!"
            else -> "Controlled movement"
        }
    }

    override fun detectMovementPhase(pose: PoseDetectionResult.Success): MovementPhase {
        val elbow = elbowSmoother.lastValue
        return when {
            elbow >= ELBOW_LOCKOUT_TARGET -> MovementPhase.LOCKOUT
            currentState == State.DESCENT -> MovementPhase.DESCENT
            currentState == State.BOTTOM || elbow < ELBOW_BOTTOM_LIMIT -> MovementPhase.BOTTOM
            else -> MovementPhase.ASCENT
        }
    }

    override fun getFormDiagnostics(pose: PoseDetectionResult.Success): List<FormDiagnostic> {
        val flare = calculateElbowFlare(pose)
        val elbow = elbowSmoother.lastValue

        return listOf(
            FormDiagnostic(
                issue = if (flare > FLARE_LIMIT_HIGH) "Excessive Elbow Flare" else "Good Elbow Path",
                angle = "Elbow Flare",
                measured = flare,
                expected = "45-75°",
                severity = if (flare > FLARE_LIMIT_HIGH) FormIssueSeverity.MODERATE else FormIssueSeverity.GOOD
            ),
            FormDiagnostic(
                issue = "Depth Achievement",
                angle = "Elbow Angle",
                measured = elbow,
                expected = "< 90°",
                severity = if (elbow < 95f) FormIssueSeverity.GOOD else FormIssueSeverity.MODERATE
            )
        )
    }

    private fun completeRep(time: Long) {
        lastFormScore = calculateFormScore()
        lastRepTime = time
        resetRepMetrics()
        currentState = State.START
    }

    private fun resetRepMetrics() {
        repMinElbowAngle = 180f
        repMaxFlareAngle = 0f
        repMaxLockoutAngle = 0f
    }

    override fun reset() {
        currentState = State.START
        lastRepTime = 0L
        resetRepMetrics()
        elbowSmoother.reset()
    }

    private fun getBilateralAngle(pose: PoseDetectionResult.Success, a: Int, b: Int, c: Int): Float? {
        val left = AngleCalculator.calculateAngle(pose.getLandmark(a), pose.getLandmark(b), pose.getLandmark(c))
        val right = AngleCalculator.calculateAngle(pose.getLandmark(a + 1), pose.getLandmark(b + 1), pose.getLandmark(c + 1))
        return if (left != null && right != null) (left + right) / 2f else left ?: right
    }

    override fun formScore(): Float = lastFormScore
    override fun hadGoodForm(): Boolean = lastFormScore >= 70f
}