package com.sovereignops.updateauditor.audit

import com.sovereignops.updateauditor.model.SignerInfo
import com.sovereignops.updateauditor.testing.Fixtures.DIGEST_A
import com.sovereignops.updateauditor.testing.Fixtures.DIGEST_B
import com.sovereignops.updateauditor.testing.Fixtures.signers
import org.junit.Assert.assertEquals
import org.junit.Test

class SignerSummaryTest {
    @Test
    fun `summarises how a package is signed`() {
        assertEquals(SignerSummary.PLATFORM, SignerSummary.of(signers(DIGEST_A, platform = true)))
        assertEquals(SignerSummary.SINGLE, SignerSummary.of(signers(DIGEST_A)))
        assertEquals(SignerSummary.SINGLE_ROTATED, SignerSummary.of(signers(DIGEST_B, past = listOf(DIGEST_A))))
        assertEquals(SignerSummary.MULTIPLE, SignerSummary.of(signers(DIGEST_A, DIGEST_B)))
        assertEquals(SignerSummary.UNAVAILABLE, SignerSummary.of(SignerInfo.NONE))
    }
}
