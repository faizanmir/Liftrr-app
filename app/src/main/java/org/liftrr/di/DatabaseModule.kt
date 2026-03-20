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
import org.liftrr.data.local.sync.SyncQueueDao
import org.liftrr.data.local.user.UserDao
import org.liftrr.data.local.user.UserProfileDao
import org.liftrr.data.local.workout.WeightsDao
import org.liftrr.data.local.workout.WorkoutDao
import org.liftrr.domain.auth.AuthRepository
import org.liftrr.data.repository.RemoteAuthRepository
import org.liftrr.data.repository.UserProfileRepositoryImpl
import org.liftrr.data.repository.UserWeightRepositoryImpl
import org.liftrr.data.repository.WorkoutRepositoryImpl
import org.liftrr.domain.user.UserProfileRepository
import org.liftrr.domain.weight.UserWeightRepository
import org.liftrr.domain.workout.WorkoutRepository
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
    fun provideWorkoutDao(database: LiftrrDb): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideUserProfileDao(database: LiftrrDb): UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    fun provideSyncQueueDao(database: LiftrrDb): SyncQueueDao {
        return database.syncQueueDao()
    }
    @Provides
    fun provideUserWeightsDao(database: LiftrrDb): WeightsDao {
        return database.weightDao()
    }
}

@InstallIn(SingletonComponent::class)
@Module
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: RemoteAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindUserWeightRepository(impl: UserWeightRepositoryImpl): UserWeightRepository
}
