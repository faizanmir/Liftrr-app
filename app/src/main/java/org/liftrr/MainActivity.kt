package org.liftrr

import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import org.liftrr.ui.screens.LiftrrApp
import org.liftrr.ui.screens.SplashViewModel
import java.security.MessageDigest

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hide ActionBar
        supportActionBar?.hide()
        actionBar?.hide()

        enableEdgeToEdge()

        // Keep splash visible while loading
        splashScreen.setKeepOnScreenCondition {
            splashViewModel.isLoading.value
        }

        // Custom exit animation
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val fadeOut = ObjectAnimator.ofFloat(
                splashScreenView.view, View.ALPHA, 1f, 0f
            )
            fadeOut.duration = 100L
            fadeOut.start()

            fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    splashScreenView.remove()
                }
            })

            fadeOut.start()
        }


        setContent {
            LiftrrApp()
        }
    }
}
