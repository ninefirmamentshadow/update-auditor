package com.sovereignops.updateauditor.ui

import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import com.sovereignops.updateauditor.MainActivity
import com.sovereignops.updateauditor.PackageDetailActivity
import com.sovereignops.updateauditor.R
import com.sovereignops.updateauditor.harness.FixedInventory
import com.sovereignops.updateauditor.harness.InventoryRule
import com.sovereignops.updateauditor.harness.settle
import com.sovereignops.updateauditor.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MainInteractionTest {
    private val fixed: FixedInventory = FixedInventory(Fixtures.device(), Fixtures.inventory())

    @get:Rule
    val inventory: InventoryRule = InventoryRule(fixed)

    private fun ActivityScenario<MainActivity>.ready(): Unit =
        settle { it.findViewById<Button>(R.id.refreshButton).isEnabled && it.shownPackages() == Fixtures.inventory().size }

    /** Rows shown: the list holds one device card plus one row per visible package. */
    private fun MainActivity.shownPackages(): Int =
        findViewById<RecyclerView>(R.id.packageList).adapter!!.itemCount - 1

    /** The list diff runs on a background thread, so wait for it instead of assuming it is done. */
    private fun ActivityScenario<MainActivity>.awaitShown(count: Int): Unit =
        settle { it.shownPackages() == count }

    @Test
    fun showsEveryPackageAndTheCounts() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.ready()
            scenario.onActivity { activity ->
                assertEquals(Fixtures.inventory().size, activity.shownPackages())
                val summary = activity.findViewById<TextView>(R.id.summaryText).text.toString()
                assertTrue(summary, summary.contains("Packages ${Fixtures.inventory().size}"))
                assertTrue(summary, summary.contains("unverified 1"))
            }
        }
    }

    @Test
    fun searchNarrowsTheListAndClearingRestoresIt() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.ready()
            scenario.onActivity { it.findViewById<EditText>(R.id.searchInput).setText("settings") }
            scenario.awaitShown(1)
            scenario.onActivity { activity ->
                assertEquals(1, activity.shownPackages())
                assertTrue(activity.findViewById<TextView>(R.id.summaryText).text.toString().contains("Showing 1 of"))
            }
            scenario.onActivity { it.findViewById<EditText>(R.id.searchInput).setText("") }
            scenario.awaitShown(Fixtures.inventory().size)
            scenario.onActivity { assertEquals(Fixtures.inventory().size, it.shownPackages()) }
        }
    }

    @Test
    fun noMatchShowsTheEmptyMessage() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.ready()
            scenario.onActivity { it.findViewById<EditText>(R.id.searchInput).setText("zzz-no-such-package") }
            scenario.awaitShown(0)
            scenario.onActivity { activity ->
                val empty = activity.findViewById<TextView>(R.id.emptyText)
                assertEquals(android.view.View.VISIBLE, empty.visibility)
                assertEquals(activity.getString(R.string.empty_no_match), empty.text.toString())
            }
        }
    }

    @Test
    fun filterChipsSelectOneFilterAtATime() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.ready()
            scenario.onActivity { it.findViewById<RadioButton>(R.id.filterUnverified).performClick() }
            scenario.awaitShown(1)
            scenario.onActivity { activity ->
                assertEquals(1, activity.shownPackages())
                assertFalse(activity.findViewById<RadioButton>(R.id.filterAll).isChecked)
            }
            scenario.onActivity { it.findViewById<RadioButton>(R.id.filterSideloaded).performClick() }
            scenario.awaitShown(2)
            scenario.onActivity { activity ->
                assertEquals(2, activity.shownPackages())
                assertFalse(activity.findViewById<RadioButton>(R.id.filterUnverified).isChecked)
            }
        }
    }

    @Test
    fun tappingARowOpensItsDetailScreen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.ready()
            scenario.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.packageList)
                list.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(4000, android.view.View.MeasureSpec.EXACTLY),
                )
                list.layout(0, 0, 1080, 4000)
                val row = list.findViewHolderForAdapterPosition(1)!!.itemView
                row.performClick()
                val started = shadowOf(activity).nextStartedActivity
                assertEquals(PackageDetailActivity::class.java.name, started.component!!.className)
                assertEquals(Fixtures.inventory()[0].packageName, started.getStringExtra(PackageDetailActivity.EXTRA_PACKAGE))
            }
        }
    }

    @Test
    fun rotationKeepsTheViewModelSoThereIsNoRescan() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.ready()
            val readsBefore = fixed.packageReads.get()
            scenario.recreate()
            scenario.ready()
            assertEquals(readsBefore, fixed.packageReads.get())
            assertEquals(1, readsBefore)
        }
    }

    @Test
    fun refreshRescansOnce() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.ready()
            scenario.onActivity { it.findViewById<Button>(R.id.refreshButton).performClick() }
            scenario.ready()
            assertEquals(2, fixed.packageReads.get())
        }
    }
}
