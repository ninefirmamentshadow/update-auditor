package com.sovereignops.updateauditor.audit

import android.annotation.SuppressLint
import android.content.pm.PackageManager

/**
 * The `PackageManager` query flags used to enumerate packages, chosen per SDK level.
 * Kept as a pure function of the SDK int so the choice is unit-tested.
 *
 * `MATCH_DISABLED_COMPONENTS` is deliberately absent: it concerns components inside a
 * package, not which packages are listed, and was previously misused here.
 */
object QueryFlags {
    // The flag constants are inlined at compile time; each is only used behind the sdkInt check.
    @SuppressLint("InlinedApi")
    @Suppress("DEPRECATION")
    fun forSdk(sdkInt: Int): Long {
        var flags = PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong() // removed-for-this-user packages
        flags = flags or if (sdkInt >= 28) {
            PackageManager.GET_SIGNING_CERTIFICATES.toLong()
        } else {
            PackageManager.GET_SIGNATURES.toLong()
        }
        if (sdkInt >= 29) flags = flags or PackageManager.MATCH_APEX.toLong() // Mainline modules
        return flags
    }
}
