package com.sovereignops.updateauditor.audit

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.sovereignops.updateauditor.model.DeviceState
import com.sovereignops.updateauditor.model.PackageState

/**
 * Where the UI gets its observations. The real implementation reads this device;
 * tests substitute fixed data so screens are deterministic. Reads can be slow and
 * must be called off the main thread.
 */
interface InventorySource {
    fun readDevice(): DeviceState
    fun readPackages(): List<PackageState>
    fun readPackage(packageName: String): PackageState?
}

/** Reads the local device. No network, no persistence, no export. */
class LocalInventory(context: Context, pins: FirstPartyPins) : InventorySource {
    private val auditor = PackageAuditor(context, pins)

    override fun readDevice(): DeviceState = DeviceAuditor.read()
    override fun readPackages(): List<PackageState> = auditor.read()
    override fun readPackage(packageName: String): PackageState? = auditor.read(packageName)
}

/** Single place the app asks for its [InventorySource]; tests override it. */
object InventoryProvider {
    private const val PINS_ASSET = "first_party_pins.txt"

    @Volatile
    @VisibleForTesting
    var override: ((Context) -> InventorySource)? = null

    fun sourceFor(context: Context): InventorySource {
        override?.let { return it(context) }
        val app = context.applicationContext
        return LocalInventory(app, loadPins(app))
    }

    /** A missing or unreadable pin file means no pins, so first-party names stay UNVERIFIED. */
    private fun loadPins(context: Context): FirstPartyPins = runCatching {
        context.assets.open(PINS_ASSET).bufferedReader().use { FirstPartyPins.parse(it.readText()).pins }
    }.getOrDefault(FirstPartyPins.NONE)
}
