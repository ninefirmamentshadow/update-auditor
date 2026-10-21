package com.sovereignops.updateauditor.ui

import android.app.Activity
import android.view.View
import android.widget.Button
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.sovereignops.updateauditor.MainActivity
import com.sovereignops.updateauditor.PackageDetailActivity
import com.sovereignops.updateauditor.R
import com.sovereignops.updateauditor.harness.FixedInventory
import com.sovereignops.updateauditor.harness.InventoryRule
import com.sovereignops.updateauditor.harness.settle
import com.sovereignops.updateauditor.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Edge-to-edge regression test. At targetSdk 35 the window draws under the system bars,
 * so the header and the list must pad themselves. Synthetic status-bar, navigation-bar,
 * cutout and keyboard insets are dispatched and the resulting padding is asserted.
 */
@RunWith(RobolectricTestRunner::class)
class InsetsTest {
    @get:Rule
    val inventory = InventoryRule(FixedInventory(Fixtures.device(), Fixtures.inventory()))

    private val statusBar = 72
    private val navigationBar = 96

    private fun insets(
        status: Int = statusBar,
        navigation: Int = navigationBar,
        cutoutSide: Int = 0,
        keyboard: Int = 0,
    ): WindowInsetsCompat = WindowInsetsCompat.Builder()
        .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, status, 0, 0))
        .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navigation))
        .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(cutoutSide, 0, cutoutSide, 0))
        .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, keyboard))
        .build()

    private fun dispatch(activity: Activity, windowInsets: WindowInsetsCompat) {
        val root = activity.findViewById<View>(R.id.root)
        ViewCompat.dispatchApplyWindowInsets(root, windowInsets)
    }

    @Test
    fun mainScreenPadsHeaderBelowStatusBarAndListAboveNavigationBar() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.settle { it.findViewById<Button>(R.id.refreshButton).isEnabled }
            scenario.onActivity { activity ->
                val header = activity.findViewById<View>(R.id.header)
                val list = activity.findViewById<View>(R.id.packageList)
                val density = RuntimeEnvironment.getApplication().resources.displayMetrics.density
                val headerTop = (12 * density).toInt()
                val listBottom = (8 * density).toInt()

                dispatch(activity, insets())
                assertEquals(headerTop + statusBar, header.paddingTop)
                assertEquals(listBottom + navigationBar, list.paddingBottom)

                // A display cutout on both sides pads both views horizontally.
                dispatch(activity, insets(cutoutSide = 40))
                assertEquals((16 * density).toInt() + 40, header.paddingLeft)
                assertEquals((16 * density).toInt() + 40, list.paddingRight)

                // An open keyboard that is taller than the navigation bar wins at the bottom.
                dispatch(activity, insets(keyboard = 600))
                assertEquals(listBottom + 600, list.paddingBottom)

                // Insets are replaced, not accumulated, when they change.
                dispatch(activity, insets(status = 0, navigation = 0))
                assertEquals(headerTop, header.paddingTop)
                assertEquals(listBottom, list.paddingBottom)
            }
        }
    }

    @Test
    fun detailScreenPadsToo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch<PackageDetailActivity>(PackageDetailActivity.intent(context, "com.example.app")).use { scenario ->
            scenario.onActivity { activity ->
                val header = activity.findViewById<View>(R.id.detailHeader)
                val scroll = activity.findViewById<View>(R.id.detailScroll)
                val density = RuntimeEnvironment.getApplication().resources.displayMetrics.density
                dispatch(activity, insets())
                assertEquals((12 * density).toInt() + statusBar, header.paddingTop)
                assertEquals((8 * density).toInt() + navigationBar, scroll.paddingBottom)
            }
        }
    }
}
