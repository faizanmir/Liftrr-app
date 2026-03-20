package org.liftrr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.liftrr.data.local.sync.SyncQueueDao
import org.liftrr.data.local.user.UserDao
import org.liftrr.data.local.user.UserProfileDao
import org.liftrr.data.local.workout.WeightsDao
import org.liftrr.data.local.workout.WorkoutDao
import org.liftrr.data.models.dto.SyncQueueItem
import org.liftrr.data.models.dto.UserDto
import org.liftrr.data.models.dto.UserProfileEntity
import org.liftrr.data.models.dto.UserWeightDto
import org.liftrr.data.models.dto.WorkoutSessionEntity

@Database(
    entities = [
        UserDto::class,
        WorkoutSessionEntity::class,
        SyncQueueItem::class,
        UserProfileEntity::class,
        UserWeightDto::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LiftrrDb : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun weightDao(): WeightsDao
}
