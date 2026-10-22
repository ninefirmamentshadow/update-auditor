package com.sovereignops.updateauditor.status

import com.sovereignops.updateauditor.upstream.samsung.SamsungRelease
import java.time.Instant
import java.time.LocalDate

object VerdictEngine {
    fun samsungFirmware(
        localBuild: String,
        localSecurityPatch: String,
        releases: List<SamsungRelease>,
        source: String,
        checkedAt: Instant,
    ): ChannelStatus {
        if (releases.isEmpty()) {
            return ChannelStatus(
                channel = UpdateChannel.SAMSUNG_FIRMWARE,
                verdict = UpdateVerdict.ERROR,
                localValue = localBuild,
                source = source,
                checkedAt = checkedAt,
                detail = "Samsung source returned no valid release entries.",
            )
        }

        val newest = releases.first()
        val localIndex = releases.indexOfFirst { it.buildNumber == localBuild }

        if (localIndex == 0) {
            return ChannelStatus(
                channel = UpdateChannel.SAMSUNG_FIRMWARE,
                verdict = UpdateVerdict.VERIFIED_CURRENT,
                localValue = localBuild,
                upstreamValue = newest.buildNumber,
                source = source,
                checkedAt = checkedAt,
                detail = "Installed firmware exactly matches the newest applicable Samsung history entry.",
            )
        }

        if (localIndex > 0) {
            return ChannelStatus(
                channel = UpdateChannel.SAMSUNG_FIRMWARE,
                verdict = UpdateVerdict.VERIFIED_UPDATE_AVAILABLE,
                localValue = localBuild,
                upstreamValue = newest.buildNumber,
                source = source,
                checkedAt = checkedAt,
                detail = "Installed firmware matches an older Samsung history entry. This does not prove immediate OTA eligibility.",
            )
        }

        val localPatch = localSecurityPatch.toLocalDateOrNull()
        val newestPatch = newest.securityPatchLevel.toLocalDateOrNull()

        if (localPatch != null && newestPatch != null && localPatch.isAfter(newestPatch)) {
            return ChannelStatus(
                channel = UpdateChannel.SAMSUNG_FIRMWARE,
                verdict = UpdateVerdict.LOCAL_AHEAD_OF_SOURCE,
                localValue = localBuild,
                upstreamValue = newest.buildNumber,
                source = source,
                checkedAt = checkedAt,
                detail = "Local firmware build is absent from the source and its security patch is newer than the newest source entry.",
            )
        }

        return ChannelStatus(
            channel = UpdateChannel.SAMSUNG_FIRMWARE,
            verdict = UpdateVerdict.UNVERIFIED,
            localValue = localBuild,
            upstreamValue = newest.buildNumber,
            source = source,
            checkedAt = checkedAt,
            detail = "Local firmware build is absent from the Samsung history. Build strings are not ordered lexicographically.",
        )
    }

    private fun String.toLocalDateOrNull(): LocalDate? =
        runCatching { LocalDate.parse(this) }.getOrNull()
}
