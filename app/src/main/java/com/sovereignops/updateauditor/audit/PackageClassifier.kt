package com.sovereignops.updateauditor.audit

import com.sovereignops.updateauditor.model.PackageOrigin

/**
 * Deterministic provenance classification. Pure Kotlin: it takes observations and
 * decides, with no Android or network access, so it is unit-testable and its policy
 * is auditable in one place.
 */
object PackageClassifier {

    /** What the classifier needs to know about one package. */
    data class Input(
        val packageName: String,
        val isSystemApp: Boolean,
        /** `checkSignatures(pkg, "android") == SIGNATURE_MATCH`. */
        val platformSigned: Boolean,
        /** SHA-256 digests (lowercase hex) of every current signer. */
        val signerDigests: List<String>,
    )

    /**
     * - A first-party package **name** is only a claim. It becomes FIRST_PARTY when the
     *   package is platform-signed, or every current signer is pinned for that package.
     *   Otherwise it is UNVERIFIED (a spoof and a not-yet-pinned genuine app look the
     *   same from here, so neither is promoted).
     * - Any other system package is SYSTEM; everything else is USER.
     *
     * With several signers every one must be pinned: a signer the owner never pinned
     * means the package is not the artifact the pin describes.
     */
    fun classify(input: Input, pins: FirstPartyPins = FirstPartyPins.NONE): PackageOrigin {
        if (pins.isFirstPartyName(input.packageName)) {
            return if (isVerified(input, pins)) PackageOrigin.FIRST_PARTY else PackageOrigin.UNVERIFIED
        }
        return if (input.isSystemApp) PackageOrigin.SYSTEM else PackageOrigin.USER
    }

    private fun isVerified(input: Input, pins: FirstPartyPins): Boolean {
        if (input.platformSigned) return true
        val pinned = pins.pinnedDigests(input.packageName)
        return pinned.isNotEmpty() &&
            input.signerDigests.isNotEmpty() &&
            input.signerDigests.all { it in pinned }
    }
}
