package com.sovereignops.updateauditor.audit

import java.util.Locale

object FirmwareBuildResolver {
    fun resolve(
        model: String,
        incremental: String,
        display: String,
        fingerprint: String,
        baseband: String,
    ): String {
        val expectedPrefix = model
            .uppercase(Locale.US)
            .removePrefix("SM-")

        // Android build fingerprints are shaped roughly as:
        // brand/product/device:release/id/incremental:type/tags
        // Isolate the build section before taking the final slash-delimited value.
        val fingerprintBuild = fingerprint
            .substringAfter(':', missingDelimiterValue = "")
            .substringBefore(':')
            .substringAfterLast('/')
            .trim()

        val radioBuild = baseband
            .substringBefore(',')
            .trim()

        return listOf(incremental, display, fingerprintBuild, radioBuild)
            .asSequence()
            .map(String::trim)
            .filter { it.isNotBlank() && !it.equals("Unavailable", ignoreCase = true) }
            .firstOrNull { candidate ->
                expectedPrefix.isNotBlank() &&
                    candidate.uppercase(Locale.US).startsWith(expectedPrefix)
            }
            ?: "Unavailable"
    }
}
