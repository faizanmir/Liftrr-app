package org.liftrr.domain.workout

import org.liftrr.domain.analytics.WorkoutReport
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Temporary holder for workout report to pass between screens
 * This is a simple singleton that holds the last completed workout report
 */
@Singleton
class WorkoutReportHolder @Inject constructor() {

    private var currentReport: WorkoutReport? = null

    fun setReport(report: WorkoutReport) {
        currentReport = report
    }

    fun getReport(): WorkoutReport? {
        return currentReport
    }

    fun clearReport() {
        currentReport = null
    }
}
