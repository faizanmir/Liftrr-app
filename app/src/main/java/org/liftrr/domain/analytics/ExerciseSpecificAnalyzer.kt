package org.liftrr.domain.analytics

import org.liftrr.ml.PoseDetectionResult

interface ExerciseSpecificAnalyzer {
    fun analyze(frames: List<PoseDetectionResult.Success>): ExerciseSpecificMetrics
}
