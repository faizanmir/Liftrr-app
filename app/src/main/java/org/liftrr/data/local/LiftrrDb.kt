package org.liftrr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.liftrr.data.models.UserDto
import org.liftrr.data.models.WorkoutSessionEntity

@Database(entities = [UserDto::class, WorkoutSessionEntity::class], version = 6, exportSchema = false)
abstract class LiftrrDb : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao
}
