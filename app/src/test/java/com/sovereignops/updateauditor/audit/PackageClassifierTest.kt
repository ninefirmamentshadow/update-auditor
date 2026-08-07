package com.sovereignops.updateauditor.audit

import com.sovereignops.updateauditor.model.PackageOrigin
import org.junit.Assert.assertEquals
import org.junit.Test

class PackageClassifierTest {
    @Test
    fun `known sovereign ops packages are first party`() {
        assertEquals(
            PackageOrigin.FIRST_PARTY,
            PackageClassifier.classify("com.sovereignops.fieldwatch", isSystemApp = false),
        )
        assertEquals(
            PackageOrigin.FIRST_PARTY,
            PackageClassifier.classify("com.drafts.compose.debug", isSystemApp = false),
        )
    }

    @Test
    fun `system package is system when not first party`() {
        assertEquals(
            PackageOrigin.SYSTEM,
            PackageClassifier.classify("com.android.settings", isSystemApp = true),
        )
    }

    @Test
    fun `ordinary installed app is user`() {
        assertEquals(
            PackageOrigin.USER,
            PackageClassifier.classify("com.example.app", isSystemApp = false),
        )
    }
}
