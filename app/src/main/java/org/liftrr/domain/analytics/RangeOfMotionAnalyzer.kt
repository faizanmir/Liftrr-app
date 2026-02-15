package org.liftrr.domain.analytics

import kotlin.math.pow
import kotlin.math.sqrt

class RangeOfMotionAnalyzer {

    fun analyze(analyses: List<RepAnalysis>): RangeOfMotionAnalysis {
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

    private fun calculateConsistency(values: List<Float>): Float {
        if (values.size < 2) return 100f
        val avg = values.average().toFloat()
        val stdDev = sqrt(values.map { (it - avg).toDouble().pow(2.0) }.average()).toFloat()
        return (100f - (stdDev / avg.coerceAtLeast(1f) * 100f)).coerceIn(0f, 100f)
    }
}
