package com.sovereignops.updateauditor.audit

/**
 * Which package names are claimed as first-party, and which signer digests are
 * pinned for them. A name alone proves nothing (anyone can ship that package
 * name); a pinned digest or the platform signature is what turns a name match into
 * [com.sovereignops.updateauditor.model.PackageOrigin.FIRST_PARTY].
 *
 * Pins come from `assets/first_party_pins.txt`, owner-maintained, one
 * `package sha256` pair per line. With no pins every first-party name stays
 * UNVERIFIED, which is the safe default.
 */
class FirstPartyPins(private val digestsByPackage: Map<String, Set<String>>) {

    fun isFirstPartyName(packageName: String): Boolean = packageName in FIRST_PARTY_NAMES

    fun pinnedDigests(packageName: String): Set<String> = digestsByPackage[packageName].orEmpty()

    /** Result of parsing the pin file. [rejectedLines] are 1-based line numbers that were ignored. */
    data class Parsed(val pins: FirstPartyPins, val rejectedLines: List<Int>)

    companion object {
        val NONE = FirstPartyPins(emptyMap())

        /** Known first-party package names, including their debug variants. */
        val FIRST_PARTY_NAMES: Set<String> = setOf(
            "com.clocktools.timetable",
            "com.clocktools.timetable.debug",
            "com.drafts.compose",
            "com.drafts.compose.debug",
            "com.sovereignops.fieldwatch",
            "com.sovereignops.fieldwatch.debug",
            "com.sovereignops.ledger",
            "com.sovereignops.ledger.debug",
            "com.sovereignops.updateauditor",
            "com.sovereignops.updateauditor.debug",
        )

        /**
         * Parses pin-file text. Blank lines and `#` comments are skipped. A line is
         * `<package> <sha256>`; it is rejected (and reported) when the package is not a
         * known first-party name or the digest is not a 64-digit SHA-256.
         */
        fun parse(text: String): Parsed {
            val byPackage = mutableMapOf<String, MutableSet<String>>()
            val rejected = mutableListOf<Int>()
            text.lineSequence().forEachIndexed { index, rawLine ->
                val line = rawLine.substringBefore('#').trim()
                if (line.isEmpty()) return@forEachIndexed
                val parts = line.split(Regex("\\s+"), limit = 2)
                val digest = if (parts.size == 2) CertDigest.normalize(parts[1]) else null
                if (digest == null || parts[0] !in FIRST_PARTY_NAMES) {
                    rejected += index + 1
                } else {
                    byPackage.getOrPut(parts[0]) { mutableSetOf() } += digest
                }
            }
            return Parsed(FirstPartyPins(byPackage.mapValues { it.value.toSet() }), rejected)
        }
    }
}
