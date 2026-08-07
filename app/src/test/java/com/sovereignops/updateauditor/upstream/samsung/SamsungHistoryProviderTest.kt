package com.sovereignops.updateauditor.upstream.samsung

import com.sovereignops.updateauditor.upstream.ProviderResult
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungHistoryProviderTest {
    @Test
    fun `unknown model is unsupported without network loader call`() {
        var loaderCalled = false
        val provider = SamsungHistoryProvider(
            htmlLoader = {
                loaderCalled = true
                ""
            },
        )

        val result = provider.fetch("SM-UNKNOWN")

        assertTrue(result is ProviderResult.Unsupported)
        assertEquals(false, loaderCalled)
    }

    @Test
    fun `malformed Samsung page becomes failure instead of crash`() {
        val provider = SamsungHistoryProvider(
            htmlLoader = { "<html><body>bad page</body></html>" },
            now = { Instant.parse("2026-08-07T12:00:00Z") },
        )

        val result = provider.fetch("SM-A166U")

        assertTrue(result is ProviderResult.Failure)
    }

    @Test
    fun `valid Samsung page returns parsed observations`() {
        val provider = SamsungHistoryProvider(
            htmlLoader = {
                """
                    <div>Build Number : A166USQS8DZG1</div>
                    <div>Android version : B(Android 16)</div>
                    <div>Release Date : 2026-07-30</div>
                    <div>Security patch level : 2026-07-05</div>
                """.trimIndent()
            },
            now = { Instant.parse("2026-08-07T12:00:00Z") },
        )

        val result = provider.fetch("SM-A166U")

        assertTrue(result is ProviderResult.Success)
        result as ProviderResult.Success
        assertEquals("A166USQS8DZG1", result.value.first().buildNumber)
    }
}
