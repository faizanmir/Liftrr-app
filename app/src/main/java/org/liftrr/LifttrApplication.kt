package org.liftrr

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Liftrr Application class.
 *
 * Startup optimizations:
 * - MediaPipe pre-warming handled by App Startup library (see MediaPipeInitializer)
 * - PoseDetector is lazy-loaded when first accessed (Hilt @Singleton)
 * - No eager initialization in Application.onCreate() for faster startup
 */
@HiltAndroidApp
class LifttrApplication : Application() {
    // Intentionally empty - all initialization handled by:
    // 1. App Startup library (MediaPipeInitializer)
    // 2. Hilt lazy injection
    // 3. ViewModel initialization when screens are opened
}
