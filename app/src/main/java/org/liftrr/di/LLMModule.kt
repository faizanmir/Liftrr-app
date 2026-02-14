package org.liftrr.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.liftrr.ml.WorkoutLLM
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LLMModule {

    @Provides
    @Singleton
    fun provideWorkoutLLM(
        @ApplicationContext context: Context
    ): WorkoutLLM {
        return WorkoutLLM(context)
    }
}
