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
 * Navigation benchmark
 *
 * Measures the performance of navigating between main screens:
 * - Home → History
 * - Home → Analytics
 * - Home → Settings
 */
@RunWith(AndroidJUnit4::class)
class NavigationBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun navigationNoCompilation() = navigation(CompilationMode.None())

    @Test
    fun navigationBaselineProfile() = navigation(
        CompilationMode.Partial(BaselineProfileMode.Require)
    )

    private fun navigation(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = "org.liftrr",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                device.waitForIdle()
            }
        ) {
            // Navigate to History screen
            val historyItem = device.findObject(By.text("History"))
                ?: device.findObject(By.desc("History"))
            historyItem?.click()
            device.waitForIdle()

            // Navigate to Analytics screen
            val analyticsItem = device.findObject(By.text("Analytics"))
                ?: device.findObject(By.desc("Analytics"))
            analyticsItem?.click()
            device.waitForIdle()

            // Navigate to Settings screen
            val settingsItem = device.findObject(By.text("Settings"))
                ?: device.findObject(By.desc("Settings"))
            settingsItem?.click()
            device.waitForIdle()

            // Navigate back to Home
            val homeItem = device.findObject(By.text("Home"))
                ?: device.findObject(By.desc("Home"))
            homeItem?.click()
            device.waitForIdle()
        }
    }
}
