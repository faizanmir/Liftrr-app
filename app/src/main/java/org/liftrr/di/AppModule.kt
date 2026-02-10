package org.liftrr.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.liftrr.BuildConfig
import org.liftrr.data.preferences.ThemePreferences
import org.liftrr.di.annotations.creds.GoogleSignInClientId
import org.liftrr.utils.DefaultDispatcherProvider
import org.liftrr.utils.DispatcherProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider



}


@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {

    @Provides
    @Singleton
    fun provideCoroutineScope(dispatcherProvider: DispatcherProvider): CoroutineScope {
        return CoroutineScope(SupervisorJob() + dispatcherProvider.io + CoroutineName("AppCoroutineScope"))
    }


    @Provides
    @Singleton
    @GoogleSignInClientId
    fun providesGoogleSignInClientId() = BuildConfig.WEB_CLIENT_ID

    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext context: Context): ThemePreferences {
        return ThemePreferences(context)
    }

}