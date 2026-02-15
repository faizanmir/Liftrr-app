package org.liftrr.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.liftrr.data.local.DatabaseMigrations
import org.liftrr.data.local.LiftrrDb
import org.liftrr.data.local.UserDao
import org.liftrr.data.repository.AuthRepository
import org.liftrr.data.repository.LocalAuthRepository
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLiftrrDatabase(@ApplicationContext context: Context): LiftrrDb {
        return Room.databaseBuilder(
            context,
            LiftrrDb::class.java,
            "liftrr-db"
        )
            .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    fun provideUserDao(database: LiftrrDb): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideWorkoutDao(database: LiftrrDb): org.liftrr.data.local.WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideSyncQueueDao(database: LiftrrDb): org.liftrr.data.local.SyncQueueDao {
        return database.syncQueueDao()
    }

    @Provides
    fun provideUserPromptDao(database: LiftrrDb): org.liftrr.data.local.UserPromptDao {
        return database.userPromptDao()
    }
}

@InstallIn(SingletonComponent::class)
@Module
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: LocalAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: org.liftrr.data.repository.WorkoutRepositoryImpl): org.liftrr.data.repository.WorkoutRepository
}
