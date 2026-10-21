package com.sovereignops.updateauditor.audit

import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("DEPRECATION")
class QueryFlagsTest {
    private fun has(flags: Long, flag: Int) = flags and flag.toLong() == flag.toLong()

    @Test
    fun `old platforms read signatures and cannot see APEX`() {
        val flags = QueryFlags.forSdk(26)
        assertTrue(has(flags, PackageManager.GET_SIGNATURES))
        assertTrue(has(flags, PackageManager.MATCH_UNINSTALLED_PACKAGES))
        assertEquals(false, has(flags, PackageManager.GET_SIGNING_CERTIFICATES))
        assertEquals(false, has(flags, PackageManager.MATCH_APEX))
    }

    @Test
    fun `Android 9 reads the signing lineage`() {
        val flags = QueryFlags.forSdk(28)
        assertTrue(has(flags, PackageManager.GET_SIGNING_CERTIFICATES))
        assertEquals(false, has(flags, PackageManager.GET_SIGNATURES))
        assertEquals(false, has(flags, PackageManager.MATCH_APEX))
    }

    @Test
    fun `Android 10 and later also list Mainline modules`() {
        for (sdk in listOf(29, 34, 35)) {
            val flags = QueryFlags.forSdk(sdk)
            assertTrue(has(flags, PackageManager.MATCH_APEX))
            assertTrue(has(flags, PackageManager.GET_SIGNING_CERTIFICATES))
            assertTrue(has(flags, PackageManager.MATCH_UNINSTALLED_PACKAGES))
        }
    }

    @Test
    fun `the component-matching flag is not used to list packages`() {
        for (sdk in listOf(26, 28, 29, 35)) {
            assertEquals(false, has(QueryFlags.forSdk(sdk), PackageManager.MATCH_DISABLED_COMPONENTS))
        }
    }
}
