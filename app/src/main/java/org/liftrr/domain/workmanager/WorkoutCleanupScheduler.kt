package org.liftrr.domain.workmanager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for workout cleanup jobs using WorkManager
 */
@Singleton
class WorkoutCleanupScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val CLEANUP_WORK_NAME = "workout_cleanup"
        private const val CLEANUP_DELAY_MINUTES = 5L // Wait 5 minutes before cleanup
    }

    /**
     * Schedule a one-time cleanup job with a delay
     * This allows time for users to undo their deletion
     */
    fun scheduleCleanup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // No network needed
            .build()

        val cleanupRequest = OneTimeWorkRequestBuilder<WorkoutCleanupWorker>()
            .setConstraints(constraints)
            .setInitialDelay(CLEANUP_DELAY_MINUTES, TimeUnit.MINUTES)
            .addTag(CLEANUP_WORK_NAME)
            .build()

        // Use REPLACE policy to restart the timer if another deletion happens
        workManager.enqueueUniqueWork(
            CLEANUP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            cleanupRequest
        )
    }
}
