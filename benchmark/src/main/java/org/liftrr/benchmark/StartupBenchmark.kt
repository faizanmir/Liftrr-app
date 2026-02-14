package org.liftrr.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Startup benchmark for Liftrr app
 *
 * Measures app startup time from tap to first frame
 * This is the most critical benchmark as it affects first impression
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() = startup(CompilationMode.None())

    @Test
    fun startupBaselineProfile() = startup(
        CompilationMode.Partial(BaselineProfileMode.Require)
    )

    private fun startup(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = "org.liftrr",
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            iterations = 3,
            startupMode = StartupMode.COLD,
            setupBlock = {
                pressHome()
            }
        ) {
            // Start the app and wait for it to be fully drawn
            startActivityAndWait()

            // Give the app time to fully initialize
            device.waitForIdle()
        }
    }
}
