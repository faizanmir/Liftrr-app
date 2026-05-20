package org.liftrr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.liftrr.data.local.workout.WeightsDao
import org.liftrr.data.local.workout.WorkoutDao
import org.liftrr.data.models.dto.UserWeightDto
import org.liftrr.data.models.dto.WorkoutSessionEntity

@Database(
    entities = [
        WorkoutSessionEntity::class,
        UserWeightDto::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LiftrrDb : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun weightDao(): WeightsDao
}
