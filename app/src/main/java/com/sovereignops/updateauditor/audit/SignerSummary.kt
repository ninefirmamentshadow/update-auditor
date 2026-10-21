package com.sovereignops.updateauditor.audit

import com.sovereignops.updateauditor.model.SignerInfo

/** A one-line description of how a package is signed, for list rows. */
enum class SignerSummary {
    PLATFORM,
    SINGLE,
    SINGLE_ROTATED,
    MULTIPLE,
    UNAVAILABLE;

    companion object {
        fun of(signers: SignerInfo): SignerSummary = when {
            signers.platformSigned -> PLATFORM
            signers.currentDigests.isEmpty() -> UNAVAILABLE
            signers.multipleSigners || signers.currentDigests.size > 1 -> MULTIPLE
            signers.rotated -> SINGLE_ROTATED
            else -> SINGLE
        }
    }
}
