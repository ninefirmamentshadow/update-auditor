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

        val fingerprintBuild = fingerprint
            .substringAfterLast('/', missingDelimiterValue = "")
            .substringBefore(':')
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
