package com.sovereignops.updateauditor.status

import com.sovereignops.updateauditor.upstream.samsung.SamsungRelease
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class VerdictEngineTest {
    private val checkedAt = Instant.parse("2026-08-07T12:00:00Z")
    private val source = "https://example.invalid/samsung"
    private val releases = listOf(
        SamsungRelease("A166USQS8DZG1", "B(Android 16)", "2026-07-30", "2026-07-05"),
        SamsungRelease("A166USQU7DZEA", "B(Android 16)", "2026-06-04", "2026-05-05"),
    )

    @Test
    fun `exact newest match is verified current`() {
        val status = VerdictEngine.samsungFirmware(
            localBuild = "A166USQS8DZG1",
            localSecurityPatch = "2026-07-05",
            releases = releases,
            source = source,
            checkedAt = checkedAt,
        )

        assertEquals(UpdateVerdict.VERIFIED_CURRENT, status.verdict)
    }

    @Test
    fun `exact older source match reports verified update available`() {
        val status = VerdictEngine.samsungFirmware(
            localBuild = "A166USQU7DZEA",
            localSecurityPatch = "2026-05-05",
            releases = releases,
            source = source,
            checkedAt = checkedAt,
        )

        assertEquals(UpdateVerdict.VERIFIED_UPDATE_AVAILABLE, status.verdict)
        assertEquals("A166USQS8DZG1", status.upstreamValue)
    }

    @Test
    fun `absent local build with newer local patch is local ahead of source`() {
        val status = VerdictEngine.samsungFirmware(
            localBuild = "UNKNOWN_NEW_BUILD",
            localSecurityPatch = "2026-08-05",
            releases = releases,
            source = source,
            checkedAt = checkedAt,
        )

        assertEquals(UpdateVerdict.LOCAL_AHEAD_OF_SOURCE, status.verdict)
    }

    @Test
    fun `absent local build with ambiguous state remains unverified`() {
        val status = VerdictEngine.samsungFirmware(
            localBuild = "UNKNOWN_BUILD",
            localSecurityPatch = "2026-07-05",
            releases = releases,
            source = source,
            checkedAt = checkedAt,
        )

        assertEquals(UpdateVerdict.UNVERIFIED, status.verdict)
    }

    @Test
    fun `build strings are never ordered lexicographically`() {
        val status = VerdictEngine.samsungFirmware(
            localBuild = "ZZZZZZZZZZ",
            localSecurityPatch = "2026-01-01",
            releases = releases,
            source = source,
            checkedAt = checkedAt,
        )

        assertEquals(UpdateVerdict.UNVERIFIED, status.verdict)
    }

    @Test
    fun `empty upstream release list is error`() {
        val status = VerdictEngine.samsungFirmware(
            localBuild = "A166USQS8DZG1",
            localSecurityPatch = "2026-07-05",
            releases = emptyList(),
            source = source,
            checkedAt = checkedAt,
        )

        assertEquals(UpdateVerdict.ERROR, status.verdict)
    }
}
