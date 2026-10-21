package com.sovereignops.updateauditor.audit

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sovereignops.updateauditor.model.PackageOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * Runs the real [PackageAuditor] against Robolectric's PackageManager with synthetic
 * packages, so the Android-facing reads (signing info, flags, install source) are
 * exercised without a device.
 */
@RunWith(AndroidJUnit4::class)
class PackageAuditorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val packageManager = context.packageManager

    private fun cert(seed: Int) = Signature(byteArrayOf(seed.toByte(), 9, 8, 7, 6))
    private fun digest(signature: Signature) = CertDigest.sha256Hex(signature.toByteArray())

    private fun install(
        packageName: String,
        signers: List<Signature>,
        past: List<Signature> = emptyList(),
        appFlags: Int = ApplicationInfo.FLAG_INSTALLED,
        installer: String? = null,
        initiating: String? = installer,
    ) {
        val info = PackageInfo().apply {
            this.packageName = packageName
            versionName = "1.0"
            firstInstallTime = 1_700_000_000_000
            lastUpdateTime = 1_700_000_100_000
            applicationInfo = ApplicationInfo().apply {
                this.packageName = packageName
                flags = appFlags
                enabled = true
                minSdkVersion = 26
                targetSdkVersion = 35
            }
            signingInfo = SigningInfo().also {
                shadowOf(it).setSignatures(signers.toTypedArray())
                shadowOf(it).setPastSigningCertificates(past.toTypedArray())
            }
        }
        shadowOf(packageManager).installPackage(info)
        if (installer != null) shadowOf(packageManager).setInstallSourceInfo(packageName, initiating, installer)
    }

    private fun auditor(pins: FirstPartyPins = FirstPartyPins.NONE) = PackageAuditor(context, pins)

    @Test
    fun `first-party name with the pinned signer reads as first party`() {
        val signer = cert(1)
        install("com.sovereignops.fieldwatch", listOf(signer))
        val pins = FirstPartyPins(mapOf("com.sovereignops.fieldwatch" to setOf(digest(signer))))

        val state = auditor(pins).read("com.sovereignops.fieldwatch")
        assertNotNull(state)
        assertEquals(PackageOrigin.FIRST_PARTY, state!!.origin)
        assertEquals(listOf(digest(signer)), state.signers.currentDigests)
        assertFalse(state.signers.multipleSigners)
        assertFalse(state.signers.platformSigned)
    }

    @Test
    fun `the same name signed by a different key is unverified`() {
        install("com.sovereignops.ledger", listOf(cert(2)))
        val pins = FirstPartyPins(mapOf("com.sovereignops.ledger" to setOf(digest(cert(1)))))
        assertEquals(PackageOrigin.UNVERIFIED, auditor(pins).read("com.sovereignops.ledger")!!.origin)
    }

    @Test
    fun `a name match with no pins stays unverified`() {
        install("com.drafts.compose", listOf(cert(3)))
        assertEquals(PackageOrigin.UNVERIFIED, auditor().read("com.drafts.compose")!!.origin)
    }

    @Test
    fun `a package sharing the platform signature is reported platform-signed`() {
        val platformKey = cert(4)
        install("android", listOf(platformKey))
        install("com.sovereignops.ledger", listOf(platformKey))
        val state = auditor().read("com.sovereignops.ledger")!!
        assertTrue(state.signers.platformSigned)
        assertEquals(PackageOrigin.FIRST_PARTY, state.origin)
    }

    @Test
    fun `multiple signers and a rotation lineage are reported`() {
        install("com.example.multi", listOf(cert(5), cert(6)))
        val multi = auditor().read("com.example.multi")!!
        assertTrue(multi.signers.multipleSigners)
        assertEquals(2, multi.signers.currentDigests.size)

        install("com.example.rotated", listOf(cert(8)), past = listOf(cert(7), cert(8)))
        val rotated = auditor().read("com.example.rotated")!!
        assertTrue(rotated.signers.rotated)
        assertEquals(listOf(digest(cert(7))), rotated.signers.pastDigests)
        assertEquals(listOf(digest(cert(8))), rotated.signers.currentDigests)
    }

    @Test
    fun `package state flags are read from application info`() {
        install(
            "com.example.updatedsystem", listOf(cert(1)),
            appFlags = ApplicationInfo.FLAG_INSTALLED or ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
        )
        val updated = auditor().read("com.example.updatedsystem")!!
        assertTrue(updated.systemApp)
        assertTrue(updated.updatedSystemApp)
        assertFalse(updated.removedForUser)
        assertEquals(PackageOrigin.SYSTEM, updated.origin)

        // Robolectric marks installed packages FLAG_INSTALLED; clear it to model "removed for this user".
        install("com.example.removed", listOf(cert(1)))
        shadowOf(packageManager).getInternalMutablePackageInfo("com.example.removed")!!
            .applicationInfo!!.flags = 0
        assertTrue(auditor().read("com.example.removed")!!.removedForUser)
    }

    @Test
    fun `install source chain is read when the platform exposes it`() {
        install("com.example.fromstore", listOf(cert(1)), installer = "com.example.store")
        val source = auditor().read("com.example.fromstore")!!.installSource
        assertEquals("com.example.store", source.installingPackage)
        assertEquals("com.example.store", source.initiatingPackage)
        assertNull(source.originatingPackage)
    }

    @Test
    fun `a package with no signing data is read without failing and is never platform-signed`() {
        val bare = PackageInfo().apply {
            packageName = "com.example.bare"
            applicationInfo = ApplicationInfo().apply { packageName = "com.example.bare"; flags = ApplicationInfo.FLAG_INSTALLED }
        }
        shadowOf(packageManager).installPackage(bare)
        val state = auditor().read("com.example.bare")!!
        assertTrue(state.signers.currentDigests.isEmpty())
        assertFalse(state.signers.platformSigned)
        assertEquals(PackageOrigin.USER, state.origin)
    }

    @Test
    fun `listing includes the installed packages and an unknown name is null`() {
        install("com.example.listed", listOf(cert(1)))
        assertTrue(auditor().read().any { it.packageName == "com.example.listed" })
        assertNull(auditor().read("com.example.not.installed"))
    }
}
