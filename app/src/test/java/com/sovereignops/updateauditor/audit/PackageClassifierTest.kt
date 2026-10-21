package com.sovereignops.updateauditor.audit

import com.sovereignops.updateauditor.model.PackageOrigin
import com.sovereignops.updateauditor.testing.Fixtures.DIGEST_A
import com.sovereignops.updateauditor.testing.Fixtures.DIGEST_B
import com.sovereignops.updateauditor.testing.Fixtures.DIGEST_C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageClassifierTest {
    private val name = "com.sovereignops.fieldwatch"
    private val pins = FirstPartyPins(
        mapOf(
            name to setOf(DIGEST_A),
            "com.drafts.compose" to setOf(DIGEST_B),
            "com.clocktools.timetable" to setOf(DIGEST_A, DIGEST_B),
        ),
    )

    private fun input(
        packageName: String = name,
        system: Boolean = false,
        platform: Boolean = false,
        vararg digests: String,
    ) = PackageClassifier.Input(packageName, system, platform, digests.toList())

    @Test
    fun `first-party name alone is unverified`() {
        assertEquals(PackageOrigin.UNVERIFIED, PackageClassifier.classify(input(), FirstPartyPins.NONE))
        assertEquals(PackageOrigin.UNVERIFIED, PackageClassifier.classify(input(digests = arrayOf(DIGEST_A)), FirstPartyPins.NONE))
    }

    @Test
    fun `first-party name with signers but no matching pin is unverified`() {
        assertEquals(PackageOrigin.UNVERIFIED, PackageClassifier.classify(input(digests = arrayOf(DIGEST_C)), pins))
    }

    @Test
    fun `first-party name with no readable signer is unverified even when pins exist`() {
        assertEquals(PackageOrigin.UNVERIFIED, PackageClassifier.classify(input(), pins))
    }

    @Test
    fun `platform-signed first-party name is first party`() {
        assertEquals(
            PackageOrigin.FIRST_PARTY,
            PackageClassifier.classify(input(platform = true, digests = arrayOf(DIGEST_C)), FirstPartyPins.NONE),
        )
    }

    @Test
    fun `pinned digest on a first-party name is first party`() {
        assertEquals(PackageOrigin.FIRST_PARTY, PackageClassifier.classify(input(digests = arrayOf(DIGEST_A)), pins))
    }

    @Test
    fun `a digest pinned for a different package does not verify this one`() {
        assertEquals(
            PackageOrigin.UNVERIFIED,
            PackageClassifier.classify(input(digests = arrayOf(DIGEST_B)), pins),
        )
    }

    @Test
    fun `multi-signer package needs every signer pinned`() {
        val timetable = "com.clocktools.timetable"
        assertEquals(
            PackageOrigin.FIRST_PARTY,
            PackageClassifier.classify(input(timetable, digests = arrayOf(DIGEST_A, DIGEST_B)), pins),
        )
        assertEquals(
            PackageOrigin.UNVERIFIED,
            PackageClassifier.classify(input(timetable, digests = arrayOf(DIGEST_A, DIGEST_C)), pins),
        )
    }

    @Test
    fun `a spoofed first-party name on a system app is unverified, not system`() {
        assertEquals(PackageOrigin.UNVERIFIED, PackageClassifier.classify(input(system = true), pins))
    }

    @Test
    fun `other system packages are system and ordinary apps are user`() {
        assertEquals(
            PackageOrigin.SYSTEM,
            PackageClassifier.classify(input("com.android.settings", system = true), pins),
        )
        assertEquals(
            PackageOrigin.USER,
            PackageClassifier.classify(input("com.example.app"), pins),
        )
    }

    @Test
    fun `platform signature does not promote a package whose name is not first party`() {
        assertEquals(
            PackageOrigin.USER,
            PackageClassifier.classify(input("com.example.app", platform = true), pins),
        )
    }

    @Test
    fun `no origin can be read as an outdated verdict`() {
        assertTrue(PackageOrigin.values().none { it.name.contains("OUTDATED", ignoreCase = true) })
    }
}
