package com.sovereignops.updateauditor.ui

import android.content.res.Resources
import com.sovereignops.updateauditor.R
import com.sovereignops.updateauditor.audit.PackageFilter
import com.sovereignops.updateauditor.audit.SignerSummary
import com.sovereignops.updateauditor.model.PackageOrigin
import com.sovereignops.updateauditor.model.PackageSource

/** Resource lookups for the enums the pure-Kotlin layer produces, so no UI text lives in that layer. */

fun Resources.originLabel(origin: PackageOrigin): String = getString(
    when (origin) {
        PackageOrigin.FIRST_PARTY -> R.string.origin_first_party
        PackageOrigin.SYSTEM -> R.string.origin_system
        PackageOrigin.USER -> R.string.origin_user
        PackageOrigin.UNVERIFIED -> R.string.origin_unverified
    },
)

fun Resources.originDetail(origin: PackageOrigin): String = getString(
    when (origin) {
        PackageOrigin.FIRST_PARTY -> R.string.origin_first_party_detail
        PackageOrigin.SYSTEM -> R.string.origin_system_detail
        PackageOrigin.USER -> R.string.origin_user_detail
        PackageOrigin.UNVERIFIED -> R.string.origin_unverified_detail
    },
)

fun Resources.signerLabel(summary: SignerSummary): String = getString(
    when (summary) {
        SignerSummary.PLATFORM -> R.string.signed_platform
        SignerSummary.SINGLE -> R.string.signed_single
        SignerSummary.SINGLE_ROTATED -> R.string.signed_rotated
        SignerSummary.MULTIPLE -> R.string.signed_multiple
        SignerSummary.UNAVAILABLE -> R.string.signed_unavailable
    },
)

fun Resources.sourceLabel(source: PackageSource): String = getString(
    when (source) {
        PackageSource.UNAVAILABLE -> R.string.source_unavailable
        PackageSource.UNSPECIFIED -> R.string.source_unspecified
        PackageSource.OTHER -> R.string.source_other
        PackageSource.STORE -> R.string.source_store
        PackageSource.LOCAL_FILE -> R.string.source_local_file
        PackageSource.DOWNLOADED_FILE -> R.string.source_downloaded_file
    },
)

/** Maps each filter to the radio button that selects it, and back. */
object FilterButtons {
    private val pairs = listOf(
        PackageFilter.ALL to R.id.filterAll,
        PackageFilter.FIRST_PARTY to R.id.filterFirstParty,
        PackageFilter.SYSTEM to R.id.filterSystem,
        PackageFilter.UPDATED_SYSTEM to R.id.filterUpdatedSystem,
        PackageFilter.USER to R.id.filterUser,
        PackageFilter.UNVERIFIED to R.id.filterUnverified,
        PackageFilter.SIDELOADED to R.id.filterSideloaded,
    )

    fun filterFor(buttonId: Int): PackageFilter = pairs.firstOrNull { it.second == buttonId }?.first ?: PackageFilter.ALL

    fun buttonFor(filter: PackageFilter): Int = pairs.first { it.first == filter }.second
}
