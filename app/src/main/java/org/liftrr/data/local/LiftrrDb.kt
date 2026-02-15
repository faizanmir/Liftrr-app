package org.liftrr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.liftrr.data.models.SyncQueueItem
import org.liftrr.data.models.UserDto
import org.liftrr.data.models.UserPromptEntity
import org.liftrr.data.models.WorkoutSessionEntity

@Database(
    entities = [
        UserDto::class,
        WorkoutSessionEntity::class,
        SyncQueueItem::class,
        UserPromptEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LiftrrDb : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun userPromptDao(): UserPromptDao
}
