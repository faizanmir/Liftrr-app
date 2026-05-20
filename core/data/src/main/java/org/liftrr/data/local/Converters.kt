package org.liftrr.data.local

import android.util.Log
import androidx.room.TypeConverter
import org.liftrr.data.local.SyncStatus
import org.liftrr.data.local.workout.WorkoutSyncOperation
import org.liftrr.data.local.workout.WorkoutSyncState

class Converters {

    private companion object {
        const val TAG = "Converters"
    }

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus =
        try {
            SyncStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Unknown SyncStatus '$value', defaulting to PENDING", e)
            SyncStatus.PENDING
        }

    @TypeConverter
    fun fromWorkoutSyncOperation(value: WorkoutSyncOperation): String = value.name

    @TypeConverter
    fun toWorkoutSyncOperation(value: String): WorkoutSyncOperation =
        try {
            WorkoutSyncOperation.valueOf(value)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Unknown WorkoutSyncOperation '$value', defaulting to UPSERT_WORKOUT", e)
            WorkoutSyncOperation.UPSERT_WORKOUT
        }

    @TypeConverter
    fun fromWorkoutSyncState(value: WorkoutSyncState): String = value.name

    @TypeConverter
    fun toWorkoutSyncState(value: String): WorkoutSyncState =
        try {
            WorkoutSyncState.valueOf(value)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Unknown WorkoutSyncState '$value', defaulting to PENDING", e)
            WorkoutSyncState.PENDING
        }
}
