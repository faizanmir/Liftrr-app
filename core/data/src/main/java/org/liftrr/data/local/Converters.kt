package org.liftrr.data.local

import androidx.room.TypeConverter
import org.liftrr.data.local.SyncStatus
import org.liftrr.data.local.workout.WorkoutSyncOperation
import org.liftrr.data.local.workout.WorkoutSyncState

class Converters {

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus =
        try { SyncStatus.valueOf(value) } catch (e: IllegalArgumentException) { SyncStatus.PENDING }

    @TypeConverter
    fun fromWorkoutSyncOperation(value: WorkoutSyncOperation): String = value.name

    @TypeConverter
    fun toWorkoutSyncOperation(value: String): WorkoutSyncOperation =
        try {
            WorkoutSyncOperation.valueOf(value)
        } catch (e: IllegalArgumentException) {
            WorkoutSyncOperation.UPSERT_WORKOUT
        }

    @TypeConverter
    fun fromWorkoutSyncState(value: WorkoutSyncState): String = value.name

    @TypeConverter
    fun toWorkoutSyncState(value: String): WorkoutSyncState =
        try {
            WorkoutSyncState.valueOf(value)
        } catch (e: IllegalArgumentException) {
            WorkoutSyncState.PENDING
        }
}
