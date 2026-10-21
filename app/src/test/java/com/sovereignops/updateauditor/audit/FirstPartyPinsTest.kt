package com.sovereignops.updateauditor.audit

import com.sovereignops.updateauditor.testing.Fixtures.DIGEST_A
import com.sovereignops.updateauditor.testing.Fixtures.DIGEST_B
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstPartyPinsTest {
    @Test
    fun `parses pins, comments and several digests per package`() {
        val text = """
            # comment line
            com.sovereignops.fieldwatch $DIGEST_A
            com.sovereignops.fieldwatch   ${DIGEST_B.uppercase()}   # rotated key

            com.drafts.compose sha256:${DIGEST_A.chunked(2).joinToString(":")}
        """.trimIndent()
        val parsed = FirstPartyPins.parse(text)
        assertTrue(parsed.rejectedLines.isEmpty())
        assertEquals(setOf(DIGEST_A, DIGEST_B), parsed.pins.pinnedDigests("com.sovereignops.fieldwatch"))
        assertEquals(setOf(DIGEST_A), parsed.pins.pinnedDigests("com.drafts.compose"))
        assertTrue(parsed.pins.pinnedDigests("com.sovereignops.ledger").isEmpty())
    }

    @Test
    fun `rejects unknown packages and malformed digests with their line numbers`() {
        val text = listOf(
            "com.example.stranger $DIGEST_A",
            "com.sovereignops.ledger tooshort",
            "com.sovereignops.ledger",
            "com.sovereignops.ledger $DIGEST_A",
        ).joinToString("\n")
        val parsed = FirstPartyPins.parse(text)
        assertEquals(listOf(1, 2, 3), parsed.rejectedLines)
        assertEquals(setOf(DIGEST_A), parsed.pins.pinnedDigests("com.sovereignops.ledger"))
        assertFalse(parsed.pins.isFirstPartyName("com.example.stranger"))
    }

    @Test
    fun `the shipped pin file is well formed and, until the owner pins, empty`() {
        val file = java.io.File("src/main/assets/first_party_pins.txt")
        val parsed = FirstPartyPins.parse(file.readText())
        assertTrue("shipped pin file has rejected lines: ${parsed.rejectedLines}", parsed.rejectedLines.isEmpty())
    }
}
