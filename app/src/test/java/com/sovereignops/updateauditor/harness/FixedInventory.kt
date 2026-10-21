package com.sovereignops.updateauditor.harness

import android.os.Looper
import androidx.test.core.app.ActivityScenario
import androidx.appcompat.app.AppCompatActivity
import com.sovereignops.updateauditor.audit.InventoryProvider
import com.sovereignops.updateauditor.audit.InventorySource
import com.sovereignops.updateauditor.model.DeviceState
import com.sovereignops.updateauditor.model.PackageState
import org.junit.rules.ExternalResource
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.atomic.AtomicInteger

/** An [InventorySource] with fixed data that counts how often it is read. */
class FixedInventory(
    private val device: DeviceState,
    private val packages: List<PackageState>,
    private val failWith: Exception? = null,
) : InventorySource {
    val deviceReads = AtomicInteger()
    val packageReads = AtomicInteger()

    override fun readDevice(): DeviceState {
        deviceReads.incrementAndGet()
        failWith?.let { throw it }
        return device
    }

    override fun readPackages(): List<PackageState> {
        packageReads.incrementAndGet()
        return packages
    }

    override fun readPackage(packageName: String): PackageState? = packages.firstOrNull { it.packageName == packageName }
}

/** Installs [source] as the app's inventory for the duration of a test, then restores the real one. */
class InventoryRule(var source: InventorySource) : ExternalResource() {
    override fun before() {
        InventoryProvider.override = { source }
    }

    override fun after() {
        InventoryProvider.override = null
    }
}

/**
 * Lets the background read and the main-thread rendering finish. The view model reads
 * on Dispatchers.IO, so poll the main looper until [done] holds.
 */
fun <A : AppCompatActivity> ActivityScenario<A>.settle(done: (A) -> Boolean) {
    repeat(400) {
        shadowOf(Looper.getMainLooper()).idle()
        var finished = false
        onActivity { finished = done(it) }
        if (finished) return
        Thread.sleep(25)
    }
    error("screen did not settle")
}
