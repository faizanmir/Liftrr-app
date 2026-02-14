package org.liftrr.domain.workmanager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.liftrr.data.repository.WorkoutRepository
import org.liftrr.utils.DispatcherProvider
import java.io.File
import kotlinx.coroutines.withContext

/**
 * WorkManager worker that cleans up deleted workouts
 * - Fetches all workouts marked as deleted
 * - Deletes their video files from storage
 * - Permanently removes them from the database
 */
@HiltWorker
class WorkoutCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val workoutRepository: WorkoutRepository,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(dispatchers.io) {
        try {
            // Get all workouts marked for deletion
            val deletedWorkouts = workoutRepository.getDeletedWorkouts()

            // Delete video files and permanently remove from database
            deletedWorkouts.forEach { workout ->
                // Delete video file if it exists
                workout.videoUri?.let { uri ->
                    try {
                        val file = File(uri)
                        if (file.exists()) {
                            val deleted = file.delete()
                            if (deleted) {
                                Log.d(
                                    "WorkoutCleanupWorker",
                                    "Deleted video file: ${file.name}"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "WorkoutCleanupWorker",
                            "Error deleting video file: ${e.message}",
                            e
                        )
                        // Continue even if file deletion fails
                    }
                }

                // Permanently delete from database
                workoutRepository.permanentlyDeleteWorkout(workout.sessionId)
            }

            Log.d(
                "WorkoutCleanupWorker",
                "Successfully cleaned up ${deletedWorkouts.size} deleted workouts"
            )

            Result.success()
        } catch (e: Exception) {
            Log.e("WorkoutCleanupWorker", "Error during cleanup: ${e.message}", e)
            Result.retry()
        }
    }
}
