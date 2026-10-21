package com.sovereignops.updateauditor.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sovereignops.updateauditor.model.InstallSource
import com.sovereignops.updateauditor.model.PackageOrigin
import com.sovereignops.updateauditor.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormat

@RunWith(AndroidJUnit4::class)
class DetailBuilderTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val dates = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    private fun rows(item: com.sovereignops.updateauditor.model.PackageState) =
        DetailBuilder.build(resources, item, dates).flatMap { it.rows }.associateBy { it.label }

    @Test
    fun epochZeroIsUnknownNotNineteenSeventy() {
        val row = rows(Fixtures.pkg(firstInstallTime = 0, lastUpdateTime = 0))
        assertEquals("unknown", row["First installed"]!!.value)
        assertEquals("unknown", row["Last updated"]!!.value)
        val known = rows(Fixtures.pkg())
        assertTrue(known["First installed"]!!.value != "unknown")
    }

    @Test
    fun everySignerDigestIsListedAndMonospaced() {
        val item = Fixtures.pkg(signers = Fixtures.signers(Fixtures.DIGEST_A, Fixtures.DIGEST_B, past = listOf(Fixtures.DIGEST_C)))
        val all = rows(item)
        assertTrue(all["Current signer 1 SHA-256"]!!.monospace)
        assertTrue(all["Current signer 2 SHA-256"]!!.monospace)
        assertTrue(all["Earlier signer 1 SHA-256"]!!.value.startsWith("CC:33:CC:33"))
        assertEquals("yes", all["Multiple signers"]!!.value)
    }

    @Test
    fun hiddenInstallSourceFieldsAreNotPresentedAsNone() {
        val all = rows(Fixtures.pkg(installSource = InstallSource.UNKNOWN))
        assertEquals("none recorded or not exposed to this app", all["Installer"]!!.value)
        assertEquals("not available on this Android version", all["Package source"]!!.value)
    }

    @Test
    fun anUnverifiedPackageExplainsMissingEvidenceAndNeverMentionsOutdated() {
        val item = Fixtures.pkg(origin = PackageOrigin.UNVERIFIED)
        val all = DetailBuilder.build(resources, item, dates).flatMap { it.rows }
        val why = all.first { it.label == "Why" }.value
        assertTrue(why, why.contains("missing evidence"))
        assertTrue(all.none { it.value.contains("outdated", ignoreCase = true) })
    }
}
