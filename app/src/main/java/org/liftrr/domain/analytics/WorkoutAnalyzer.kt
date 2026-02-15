package org.liftrr.domain.analytics

import org.liftrr.ml.PoseDetectionResult

/**
 * High-Performance Workout Analyzer
 * Processes recorded session data to generate technical insights and coaching.
 */
object WorkoutAnalyzer {

    fun analyzeSession(session: WorkoutSession): WorkoutReport {
        val repAnalyses = analyzeAllReps(session)
        val rangeOfMotionAnalyzer = RangeOfMotionAnalyzer()
        val rangeOfMotion = rangeOfMotionAnalyzer.analyze(repAnalyses)
        val tempoAnalyzer = TempoAnalyzer()
        val tempo = tempoAnalyzer.analyze(session, repAnalyses)
        val symmetryAnalyzer = SymmetryAnalyzer()
        val symmetry = symmetryAnalyzer.analyze(session)
        val formConsistencyAnalyzer = FormConsistencyAnalyzer()
        val formConsistency = formConsistencyAnalyzer.analyze(session)
        val exerciseSpecificMetrics = analyzeExerciseSpecificMetrics(session)

        val recommendationGenerator = RecommendationGenerator()
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
            recommendations = recommendationGenerator.generate(
                session, repAnalyses, rangeOfMotion, tempo, symmetry, exerciseSpecificMetrics
            ),
            exerciseSpecificMetrics = exerciseSpecificMetrics
        )
    }

    private fun analyzeAllReps(session: WorkoutSession): List<RepAnalysis> {
        val repAnalyzer = RepAnalyzer()
        return session.reps.map { rep ->
            val repFrames = session.poseFrames.filter { it.repNumber == rep.repNumber }
            repAnalyzer.analyze(rep, repFrames, session.exerciseType)
        }
    }

    private fun analyzeExerciseSpecificMetrics(session: WorkoutSession): ExerciseSpecificMetrics? {
        val frames = session.poseFrames.mapNotNull { it.poseResult as? PoseDetectionResult.Success }
        if (frames.isEmpty()) return null
        val analyzer = ExerciseAnalyzerFactory.create(session.exerciseType)
        return analyzer.analyze(frames)
    }
}
        