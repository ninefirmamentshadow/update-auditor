package com.sovereignops.updateauditor.audit

import org.junit.Assert.assertEquals
import org.junit.Test

class FirmwareBuildResolverTest {
    @Test
    fun `prefers matching Samsung incremental build`() {
        val result = FirmwareBuildResolver.resolve(
            model = "SM-A166U",
            incremental = "A166USQS8DZG1",
            display = "BP4A.251205.006",
            fingerprint = "samsung/a16xsq/a16x:16/BP4A.251205.006/A166USQS8DZG1:user/release-keys",
            baseband = "A166USQS8DZG1,A166USQS8DZG1",
        )

        assertEquals("A166USQS8DZG1", result)
    }

    @Test
    fun `falls back to matching fingerprint build`() {
        val result = FirmwareBuildResolver.resolve(
            model = "SM-A166U",
            incremental = "BP4A.251205.006",
            display = "BP4A.251205.006",
            fingerprint = "samsung/a16xsq/a16x:16/BP4A.251205.006/A166USQS8DZG1:user/release-keys",
            baseband = "Unavailable",
        )

        assertEquals("A166USQS8DZG1", result)
    }

    @Test
    fun `does not relabel unrelated build identifier as Samsung firmware`() {
        val result = FirmwareBuildResolver.resolve(
            model = "SM-A166U",
            incremental = "BP4A.251205.006",
            display = "BP4A.251205.006",
            fingerprint = "generic/device/product:16/BP4A.251205.006/123:user/release-keys",
            baseband = "Unavailable",
        )

        assertEquals("Unavailable", result)
    }
}
