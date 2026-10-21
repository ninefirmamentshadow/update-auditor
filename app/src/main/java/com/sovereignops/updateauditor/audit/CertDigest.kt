package com.sovereignops.updateauditor.audit

import java.security.MessageDigest

/** Certificate digests. Pure JVM, so it unit-tests without Android. */
object CertDigest {
    private val HEX = "0123456789abcdef".toCharArray()

    /** Lowercase hex SHA-256 of [encodedCertificate] (the DER bytes of the certificate). */
    fun sha256Hex(encodedCertificate: ByteArray): String =
        toHex(MessageDigest.getInstance("SHA-256").digest(encodedCertificate))

    fun toHex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xff
            out[i * 2] = HEX[v ushr 4]
            out[i * 2 + 1] = HEX[v and 0x0f]
        }
        return String(out)
    }

    /**
     * Normalises a digest as people paste it (colons, spaces, upper case, an optional
     * `sha256:` prefix) to lowercase hex, or null if it is not a 64-digit SHA-256.
     */
    fun normalize(raw: String): String? {
        val cleaned = raw.trim().lowercase().removePrefix("sha256:").filter { it != ':' && !it.isWhitespace() }
        return cleaned.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
    }

    /** Uppercase colon-separated bytes, [bytesPerLine] to a line, for reading on a small screen. */
    fun display(hex: String, bytesPerLine: Int = 8): String =
        hex.uppercase().chunked(2).chunked(bytesPerLine).joinToString("\n") { it.joinToString(":") }
}
