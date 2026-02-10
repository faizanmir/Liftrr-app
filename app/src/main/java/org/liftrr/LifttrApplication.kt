package org.liftrr

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.liftrr.ml.PoseDetector
import javax.inject.Inject

@HiltAndroidApp
class LifttrApplication : Application() {

    @Inject
    lateinit var poseDetector: PoseDetector

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                appScope.launch {
                    try {
                        poseDetector.initialize()
                    } catch (e: Exception) {
                        Log.e(
                            "LifttrApplication",
                            "Failed to initialize pose detector",
                            e
                        )
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                poseDetector.stop()
            }
        })
    }
}
