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

/**
 * Reads the local device. No network, no persistence, no export. The pin file is read
 * on first use, so it happens on the caller's (background) thread, not at construction.
 */
class LocalInventory(private val context: Context) : InventorySource {
    private val auditor by lazy { PackageAuditor(context, loadPins(context)) }

    override fun readDevice(): DeviceState = DeviceAuditor.read()
    override fun readPackages(): List<PackageState> = auditor.read()
    override fun readPackage(packageName: String): PackageState? = auditor.read(packageName)

    private companion object {
        const val PINS_ASSET = "first_party_pins.txt"

        /** A missing or unreadable pin file means no pins, so first-party names stay UNVERIFIED. */
        fun loadPins(context: Context): FirstPartyPins = runCatching {
            context.assets.open(PINS_ASSET).bufferedReader().use { FirstPartyPins.parse(it.readText()).pins }
        }.getOrDefault(FirstPartyPins.NONE)
    }
}

/** Single place the app asks for its [InventorySource]; tests override it. */
object InventoryProvider {
    @Volatile
    @VisibleForTesting
    var override: ((Context) -> InventorySource)? = null

    fun sourceFor(context: Context): InventorySource {
        override?.let { return it(context) }
        return LocalInventory(context.applicationContext)
    }
}
