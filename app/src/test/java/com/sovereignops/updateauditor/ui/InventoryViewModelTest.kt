package com.sovereignops.updateauditor.ui

import android.os.Looper
import com.sovereignops.updateauditor.audit.PackageFilter
import com.sovereignops.updateauditor.harness.FixedInventory
import com.sovereignops.updateauditor.model.PackageOrigin
import com.sovereignops.updateauditor.testing.Fixtures
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** The view model under Robolectric's main looper; reads run inline on [Dispatchers.Unconfined]. */
@RunWith(RobolectricTestRunner::class)
class InventoryViewModelTest {
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun scansOnceOnCreationAndPublishesTheInventory() {
        val source = FixedInventory(Fixtures.device(), Fixtures.inventory())
        val vm = InventoryViewModel(source, Dispatchers.Unconfined)
        idle()
        val state = vm.state.value
        assertFalse(state.scanning)
        assertNull(state.error)
        assertEquals(Fixtures.inventory(), state.packages)
        assertEquals(Fixtures.device(), state.device)
        assertEquals(1, source.packageReads.get())
    }

    @Test
    fun aFailedScanIsReportedByTypeOnlyAndCanBeRetried() {
        val source = FixedInventory(Fixtures.device(), emptyList(), failWith = IllegalStateException("secret detail"))
        val vm = InventoryViewModel(source, Dispatchers.Unconfined)
        idle()
        assertEquals("IllegalStateException", vm.state.value.error)
        assertFalse(vm.state.value.scanning)
        vm.refresh()
        idle()
        assertEquals(2, source.deviceReads.get())
    }

    @Test
    fun queryAndFilterReshapeTheVisibleListWithoutRescanning() {
        val source = FixedInventory(Fixtures.device(), Fixtures.inventory())
        val vm = InventoryViewModel(source, Dispatchers.Unconfined)
        idle()
        vm.setFilter(PackageFilter.UNVERIFIED)
        assertEquals(listOf("com.drafts.compose"), vm.state.value.visible.map { it.packageName })
        vm.setFilter(PackageFilter.ALL)
        vm.setQuery("dual")
        assertEquals(listOf("com.example.tool"), vm.state.value.visible.map { it.packageName })
        assertEquals(1, vm.state.value.count(PackageOrigin.UNVERIFIED))
        assertEquals(1, source.packageReads.get())
    }

    @Test
    fun detailViewModelReportsFoundMissingAndFailed() {
        val source = FixedInventory(Fixtures.device(), Fixtures.inventory())
        val found = DetailViewModel(source, "com.example.app", Dispatchers.Unconfined)
        val missing = DetailViewModel(source, "com.example.gone", Dispatchers.Unconfined)
        idle()
        assertTrue(found.state.value is DetailViewModel.UiState.Loaded)
        assertEquals(DetailViewModel.UiState.NotFound, missing.state.value)
        val failing = object : com.sovereignops.updateauditor.audit.InventorySource {
            override fun readDevice() = Fixtures.device()
            override fun readPackages() = emptyList<com.sovereignops.updateauditor.model.PackageState>()
            override fun readPackage(packageName: String): com.sovereignops.updateauditor.model.PackageState? = error("boom")
        }
        val failed = DetailViewModel(failing, "x", Dispatchers.Unconfined)
        idle()
        assertNotNull((failed.state.value as DetailViewModel.UiState.Failed).errorType)
    }
}
