package com.sovereignops.updateauditor.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CertDigestTest {
    @Test
    fun `sha256 matches the published test vectors`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            CertDigest.sha256Hex(ByteArray(0)),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            CertDigest.sha256Hex("abc".toByteArray()),
        )
    }

    @Test
    fun `normalize accepts pasted forms and rejects everything else`() {
        val hex = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        assertEquals(hex, CertDigest.normalize(hex.uppercase()))
        assertEquals(hex, CertDigest.normalize("SHA256:" + hex.chunked(2).joinToString(":")))
        assertEquals(hex, CertDigest.normalize("  $hex \n"))
        assertNull(CertDigest.normalize(hex.dropLast(2)))
        assertNull(CertDigest.normalize(hex.replaceRange(0, 1, "g")))
        assertNull(CertDigest.normalize(""))
    }

    @Test
    fun `display wraps bytes onto lines`() {
        val hex = (0 until 32).joinToString("") { "%02x".format(it) }
        val lines = CertDigest.display(hex).lines()
        assertEquals(4, lines.size)
        assertEquals("00:01:02:03:04:05:06:07", lines.first())
    }
}
