package com.sovereignops.updateauditor.audit

import com.sovereignops.updateauditor.model.InstallSource
import com.sovereignops.updateauditor.model.PackageOrigin
import com.sovereignops.updateauditor.model.PackageSource
import com.sovereignops.updateauditor.testing.Fixtures
import com.sovereignops.updateauditor.testing.Fixtures.pkg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageFilterTest {
    private val inventory = Fixtures.inventory()
    private fun names(filter: PackageFilter, query: String = "") =
        PackageFilter.apply(inventory, filter, query).map { it.packageName }

    @Test
    fun `filters by origin and flags`() {
        assertEquals(inventory.size, names(PackageFilter.ALL).size)
        assertEquals(listOf("com.clocktools.timetable"), names(PackageFilter.FIRST_PARTY))
        assertEquals(listOf("com.drafts.compose"), names(PackageFilter.UNVERIFIED))
        assertEquals(
            setOf("com.android.settings", "com.example.webview", "com.android.mediaprovider"),
            names(PackageFilter.SYSTEM).toSet(),
        )
        assertEquals(listOf("com.example.webview"), names(PackageFilter.UPDATED_SYSTEM))
        assertEquals(setOf("com.example.app", "com.example.game", "com.example.tool"), names(PackageFilter.USER).toSet())
    }

    @Test
    fun `sideloaded means no recorded installer app outside the platform`() {
        // Local file, no installer: sideloaded. Package-installer screen: sideloaded.
        assertEquals(
            setOf("com.clocktools.timetable", "com.drafts.compose"),
            names(PackageFilter.SIDELOADED).toSet(),
        )
        assertFalse(PackageFilter.isSideloaded(pkg()))
        // System and Mainline packages are never "sideloaded", whatever their installer says.
        assertFalse(PackageFilter.isSideloaded(pkg(systemApp = true, installSource = InstallSource.UNKNOWN)))
        assertFalse(PackageFilter.isSideloaded(pkg(apex = true, installSource = InstallSource.UNKNOWN)))
        assertTrue(PackageFilter.isSideloaded(pkg(installSource = Fixtures.store(source = PackageSource.DOWNLOADED_FILE))))
        assertTrue(PackageFilter.isSideloaded(pkg(installSource = InstallSource.UNKNOWN)))
    }

    @Test
    fun `search matches label, package name and installer, ignoring case`() {
        assertEquals(listOf("com.android.settings"), names(PackageFilter.ALL, "SETT"))
        assertEquals(listOf("com.example.webview"), names(PackageFilter.ALL, "webview"))
        assertTrue(names(PackageFilter.ALL, "com.example.store").size >= 3)
        assertTrue(names(PackageFilter.ALL, "zzz-no-such").isEmpty())
        assertEquals(inventory.size, names(PackageFilter.ALL, "   ").size)
    }

    @Test
    fun `filter and search combine`() {
        assertEquals(listOf("com.example.tool"), names(PackageFilter.USER, "dual"))
        assertTrue(names(PackageFilter.FIRST_PARTY, "dual").isEmpty())
    }

    @Test
    fun `origin of the fixtures covers every bucket`() {
        assertEquals(PackageOrigin.values().toSet(), inventory.map { it.origin }.toSet())
    }
}
