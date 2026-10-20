package com.sovereignops.updateauditor.ui

import android.os.Looper
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.sovereignops.updateauditor.MainActivity
import com.sovereignops.updateauditor.R
import com.sovereignops.updateauditor.harness.ScreenCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Regression screens for the main list at 320 dp and 411 dp, font scale 1.0 and
 * 2.0, light and dark. Goldens live in app/src/test/screenshots. A normal `test`
 * run verifies against them; `./gradlew recordRoborazziDebug` re-records after an
 * intentional UI change, and the diff of the PNGs is then part of the review.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
class MainScreenTest(private val case: ScreenCase) {

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = ScreenCase.parameters()
    }

    private fun configure() {
        RuntimeEnvironment.setQualifiers(case.qualifiers)
        RuntimeEnvironment.setFontScale(case.fontScale)
    }

    private fun settle(scenario: ActivityScenario<MainActivity>) {
        repeat(400) {
            shadowOf(Looper.getMainLooper()).idle()
            var done = false
            scenario.onActivity { done = it.findViewById<Button>(R.id.refreshButton).isEnabled }
            if (done) return
            Thread.sleep(25)
        }
        error("local scan did not finish")
    }

    @Test
    fun mainScreenMatchesGolden() {
        configure()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            settle(scenario)
            onView(isRoot()).captureRoboImage("main_${case.slug}.png")
        }
    }

    @Test
    fun mainScreenPassesAccessibilityChecks() {
        configure()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            settle(scenario)
            onView(isRoot()).checkRoboAccessibility(
                RoborazziATFAccessibilityCheckOptions(
                    checker = RoborazziATFAccessibilityChecker(
                        preset = AccessibilityCheckPreset.LATEST,
                    ),
                    failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Error,
                ),
            )
        }
    }
}
