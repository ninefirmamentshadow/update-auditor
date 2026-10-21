package com.sovereignops.updateauditor.ui

import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import androidx.test.platform.app.InstrumentationRegistry
import com.sovereignops.updateauditor.PackageDetailActivity
import com.sovereignops.updateauditor.R
import com.sovereignops.updateauditor.harness.FixedInventory
import com.sovereignops.updateauditor.harness.InventoryRule
import com.sovereignops.updateauditor.harness.ScreenCase
import com.sovereignops.updateauditor.harness.settle
import com.sovereignops.updateauditor.testing.Fixtures
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.RuntimeEnvironment

/** The package detail screen over the same matrix. The subject has two signers and a debuggable flag, so it is the longest page. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
class DetailScreenTest(private val case: ScreenCase) {

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = ScreenCase.parameters()
    }

    @get:Rule
    val inventory = InventoryRule(FixedInventory(Fixtures.device(), Fixtures.inventory()))

    private fun launch(packageName: String = "com.example.tool"): ActivityScenario<PackageDetailActivity> {
        RuntimeEnvironment.setQualifiers(case.qualifiers)
        RuntimeEnvironment.setFontScale(case.fontScale)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return ActivityScenario.launch<PackageDetailActivity>(PackageDetailActivity.intent(context, packageName)).also { scenario ->
            scenario.settle { it.findViewById<android.widget.LinearLayout>(R.id.detailContent).childCount > 1 }
        }
    }

    @Test
    fun detailScreenMatchesGolden() {
        launch().use { onView(isRoot()).captureRoboImage("detail_${case.slug}.png") }
    }

    @Test
    fun detailScreenPassesAccessibilityChecks() {
        launch().use {
            onView(isRoot()).checkRoboAccessibility(
                RoborazziATFAccessibilityCheckOptions(
                    checker = RoborazziATFAccessibilityChecker(preset = AccessibilityCheckPreset.LATEST),
                    failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Error,
                ),
            )
        }
    }

    @Test
    fun digestsAreDisplayedButNotSelectable() {
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val rows = activity.findViewById<android.widget.LinearLayout>(R.id.detailContent)
                val values = (0 until rows.childCount).mapNotNull { rows.getChildAt(it).findViewById<TextView>(R.id.rowValue) }
                assertTrue(values.any { it.text.contains(Fixtures.DIGEST_A.take(2).uppercase()) })
                assertTrue(values.none { it.isTextSelectable })
            }
        }
    }
}
