package org.liftrr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.liftrr.data.local.workout.WeightsDao
import org.liftrr.data.local.workout.WorkoutDao
import org.liftrr.data.local.workout.WorkoutSyncQueueDao
import org.liftrr.data.local.workout.UserWeightDto
import org.liftrr.data.local.workout.WorkoutSessionEntity
import org.liftrr.data.local.workout.WorkoutSyncQueueEntity

@Database(
    entities = [
        WorkoutSessionEntity::class,
        UserWeightDto::class,
        WorkoutSyncQueueEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LiftrrDb : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun weightDao(): WeightsDao
    abstract fun workoutSyncQueueDao(): WorkoutSyncQueueDao
}
