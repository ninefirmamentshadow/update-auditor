package com.sovereignops.updateauditor.ui

import java.text.DateFormat
import java.util.Date

/** Formats install and update times. Android reports 0 when it has nothing; that is "unknown", not 1970. */
object TimestampText {
    fun isKnown(epochMillis: Long): Boolean = epochMillis > 0L

    fun format(epochMillis: Long, formatter: DateFormat, unknown: String): String =
        if (isKnown(epochMillis)) formatter.format(Date(epochMillis)) else unknown
}
