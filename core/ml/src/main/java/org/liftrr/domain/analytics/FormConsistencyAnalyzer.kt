package org.liftrr.domain.analytics

import org.liftrr.domain.analytics.AnalysisConstants
import kotlin.math.pow
import kotlin.math.sqrt

class FormConsistencyAnalyzer {

    fun analyze(session: WorkoutSession): FormConsistencyAnalysis {
        val qualities = session.reps.map { it.poseQuality }
        if (qualities.isNotEmpty()) qualities.average().toFloat() else 0f
        val firstHalf = qualities.take(qualities.size / 2).average()
        val secondHalf = qualities.drop(qualities.size / 2).average()
        val trend = when {
            secondHalf > firstHalf + AnalysisConstants.FORM_TREND_IMPROVEMENT_THRESHOLD -> "Improving"
            secondHalf < firstHalf - AnalysisConstants.FORM_TREND_DECLINE_THRESHOLD -> "Declining"
            else -> "Stable"
        }
        return FormConsistencyAnalysis(calculateConsistency(qualities) * 100f, trend)
    }

    private fun calculateConsistency(values: List<Float>): Float {
        if (values.size < 2) return 100f
        val avg = values.average().toFloat()
        val stdDev = sqrt(values.map { (it - avg).toDouble().pow(2.0) }.average()).toFloat()
        return (100f - (stdDev / avg.coerceAtLeast(1f) * 100f)).coerceIn(0f, 100f)
    }
}
