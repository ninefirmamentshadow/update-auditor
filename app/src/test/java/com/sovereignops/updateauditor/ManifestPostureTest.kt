package com.sovereignops.updateauditor

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the repository's security posture against a dependency's manifest merge or a
 * careless edit: no network permission, no backup, and only the one inventory permission.
 */
@RunWith(AndroidJUnit4::class)
class ManifestPostureTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun requestsOnlyTheInventoryPermission() {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions?.toList().orEmpty()
        assertFalse("INTERNET must never be requested", requested.contains("android.permission.INTERNET"))
        // androidx.core merges one signature-level permission scoped to this app's own
        // package (for non-exported dynamic receivers); it grants nothing new.
        val platformRequests = requested.filterNot { it.startsWith(context.packageName + ".") }
        assertEquals(listOf("android.permission.QUERY_ALL_PACKAGES"), platformRequests)
    }

    @Test
    fun backupAndCleartextAreOff() {
        val flags = context.applicationInfo.flags
        assertEquals(0, flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        assertEquals(0, flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC)
        assertTrue(context.applicationInfo.packageName.startsWith("com.sovereignops.updateauditor"))
    }
}
