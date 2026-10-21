package com.sovereignops.updateauditor.ui

import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
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
import com.sovereignops.updateauditor.harness.FixedInventory
import com.sovereignops.updateauditor.harness.InventoryRule
import com.sovereignops.updateauditor.harness.ScreenCase
import com.sovereignops.updateauditor.harness.settle
import com.sovereignops.updateauditor.testing.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.RuntimeEnvironment

/**
 * Regression screens for the main list at 320 dp and 411 dp, font scale 1.0 and 2.0,
 * light and dark, over fixed synthetic data. Goldens live in app/src/test/screenshots.
 * A normal `test` run verifies against them; `./gradlew recordRoborazziDebug` re-records
 * after an intentional UI change, and the PNG diff is then part of the review.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
class MainScreenTest(private val case: ScreenCase) {

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = ScreenCase.parameters()
    }

    @get:Rule
    val inventory = InventoryRule(FixedInventory(Fixtures.device(), Fixtures.inventory()))

    private fun launch(): ActivityScenario<MainActivity> {
        RuntimeEnvironment.setQualifiers(case.qualifiers)
        RuntimeEnvironment.setFontScale(case.fontScale)
        return ActivityScenario.launch(MainActivity::class.java).also { scenario ->
            // The list diff runs on a background thread, so wait for the rows as well as the button.
            scenario.settle {
                it.findViewById<Button>(R.id.refreshButton).isEnabled &&
                    it.findViewById<RecyclerView>(R.id.packageList).adapter!!.itemCount == Fixtures.inventory().size + 1
            }
        }
    }

    @Test
    fun mainScreenMatchesGolden() {
        launch().use { onView(isRoot()).captureRoboImage("main_${case.slug}.png") }
    }

    @Test
    fun mainScreenPassesAccessibilityChecks() {
        launch().use {
            onView(isRoot()).checkRoboAccessibility(
                RoborazziATFAccessibilityCheckOptions(
                    checker = RoborazziATFAccessibilityChecker(preset = AccessibilityCheckPreset.LATEST),
                    failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Error,
                ),
            )
        }
    }
}
