package org.liftrr.domain.analytics

import kotlin.math.pow
import kotlin.math.sqrt

class TempoAnalyzer {

    fun analyze(session: WorkoutSession, analyses: List<RepAnalysis>): TempoAnalysis {
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

    private fun calculateConsistency(values: List<Float>): Float {
        if (values.size < 2) return 100f
        val avg = values.average().toFloat()
        val stdDev = sqrt(values.map { (it - avg).toDouble().pow(2.0) }.average()).toFloat()
        return (100f - (stdDev / avg.coerceAtLeast(1f) * 100f)).coerceIn(0f, 100f)
    }
}
