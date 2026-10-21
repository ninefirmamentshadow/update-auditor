package com.sovereignops.updateauditor.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TimestampTextTest {
    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    @Test
    fun zeroAndNegativeAreUnknown() {
        assertFalse(TimestampText.isKnown(0))
        assertFalse(TimestampText.isKnown(-1))
        assertEquals("unknown", TimestampText.format(0, format, "unknown"))
    }

    @Test
    fun aRealTimestampIsFormatted() {
        assertTrue(TimestampText.isKnown(1))
        assertEquals("2023-11-14 22:13", TimestampText.format(1_700_000_000_000, format, "unknown"))
    }
}
