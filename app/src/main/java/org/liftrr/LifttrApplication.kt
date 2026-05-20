package org.liftrr

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import org.liftrr.BuildConfig
import javax.inject.Inject

/**
 * Liftrr Application class.
 *
 * Startup optimizations:
 * - MediaPipe pre-warming handled by App Startup library (see MediaPipeInitializer)
 * - PoseDetector is lazy-loaded when first accessed (Hilt @Singleton)
 * - No eager initialization in Application.onCreate() for faster startup
 */
@HiltAndroidApp
class LifttrApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    // App initialization is handled by:
    // 1. App Startup library (MediaPipeInitializer)
    // 2. Hilt lazy injection
    // 3. ViewModel initialization when screens are opened
}
