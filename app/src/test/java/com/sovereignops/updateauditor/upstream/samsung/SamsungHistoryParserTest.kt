package com.sovereignops.updateauditor.upstream.samsung

import org.junit.Assert.assertEquals
import org.junit.Test

class SamsungHistoryParserTest {
    @Test
    fun `parses release facts in source order and ignores prose`() {
        val html = """
            <html><body>
              <div>Build Number : A166USQS8DZG1</div>
              <div>Android version : B(Android 16)</div>
              <div>Release Date : 2026-07-30</div>
              <div>Security patch level : 2026-07-05</div>
              <p>This prose mentions security, dates, and other words but is not a release record.</p>
              <hr />
              <div>Build Number : A166USQU7DZEA</div>
              <div>Android version : B(Android 16)</div>
              <div>Release Date : 2026-06-04</div>
              <div>Security patch level : 2026-05-05</div>
            </body></html>
        """.trimIndent()

        val releases = SamsungHistoryParser.parse(html)

        assertEquals(2, releases.size)
        assertEquals("A166USQS8DZG1", releases[0].buildNumber)
        assertEquals("2026-07-05", releases[0].securityPatchLevel)
        assertEquals("A166USQU7DZEA", releases[1].buildNumber)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed page with no release entries fails closed`() {
        SamsungHistoryParser.parse("<html><body>no release history here</body></html>")
    }
}
