package org.liftrr.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Camera and Pose Detection benchmark
 *
 * Measures the performance of:
 * - Camera initialization
 * - MediaPipe pose detection model loading
 * - First frame pose analysis
 */
@RunWith(AndroidJUnit4::class)
class CameraBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun cameraInitializationNoCompilation() = cameraInitialization(CompilationMode.None())

    @Test
    fun cameraInitializationBaselineProfile() = cameraInitialization(
        CompilationMode.Partial(BaselineProfileMode.Require)
    )

    private fun cameraInitialization(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = "org.liftrr",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            startupMode = StartupMode.COLD,
            setupBlock = {
                pressHome()
            }
        ) {
            // Start the app
            startActivityAndWait()

            // Wait for home screen
            device.wait(
                Until.hasObject(By.text("Start Workout")),
                5000
            )

            // Click "Start Workout" button to open camera
            val startButton = device.findObject(By.text("Start Workout"))
            if (startButton != null) {
                startButton.click()

                // Wait for camera preview to appear
                // Adjust selector based on your camera screen composable
                device.wait(
                    Until.hasObject(By.desc("Camera Preview")),
                    10000
                ) || device.wait(
                    Until.hasObject(By.text("Rep Counter")),
                    10000
                )

                // Wait a bit for pose detection to initialize
                device.waitForIdle(3000)
            }
        }
    }
}
