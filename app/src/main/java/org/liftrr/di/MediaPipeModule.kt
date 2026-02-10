package org.liftrr.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.liftrr.ml.MediaPipePoseDetector
import org.liftrr.ml.PoseDetector
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaPipeModule {

    @Binds
    @Singleton
    abstract fun bindPoseDetector(impl: MediaPipePoseDetector): PoseDetector
}
