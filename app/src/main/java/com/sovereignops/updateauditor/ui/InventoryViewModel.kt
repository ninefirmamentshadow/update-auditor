package com.sovereignops.updateauditor.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sovereignops.updateauditor.audit.InventoryProvider
import com.sovereignops.updateauditor.audit.InventorySource
import com.sovereignops.updateauditor.audit.PackageFilter
import com.sovereignops.updateauditor.model.DeviceState
import com.sovereignops.updateauditor.model.PackageOrigin
import com.sovereignops.updateauditor.model.PackageState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the scan. It survives rotation, so the inventory is read once per launch (or per
 * explicit refresh) instead of on every configuration change, and only one scan can be in
 * flight, which removes the old worker-thread race.
 */
class InventoryViewModel(
    private val source: InventorySource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    data class UiState(
        val scanning: Boolean = true,
        val device: DeviceState? = null,
        val packages: List<PackageState> = emptyList(),
        /** Simple class name of the failure; no message text, so nothing sensitive is surfaced. */
        val error: String? = null,
        val query: String = "",
        val filter: PackageFilter = PackageFilter.ALL,
    ) {
        val visible: List<PackageState> get() = PackageFilter.apply(packages, filter, query)

        fun count(origin: PackageOrigin): Int = packages.count { it.origin == origin }
    }

    private val mutableState = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    private var scanJob: Job? = null

    /** Starts a scan unless one is already running. Safe to call from the main thread only. */
    fun refresh() {
        if (scanJob?.isActive == true) return
        mutableState.update { it.copy(scanning = true, error = null) }
        scanJob = viewModelScope.launch {
            val outcome = withContext(ioDispatcher) {
                try {
                    Result.success(source.readDevice() to source.readPackages())
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Result.failure(failure)
                }
            }
            outcome.fold(
                onSuccess = { (device, packages) ->
                    mutableState.update { it.copy(scanning = false, device = device, packages = packages, error = null) }
                },
                onFailure = { failure ->
                    mutableState.update { it.copy(scanning = false, error = failure.javaClass.simpleName) }
                },
            )
        }
    }

    fun setQuery(query: String) = mutableState.update { it.copy(query = query) }

    fun setFilter(filter: PackageFilter) = mutableState.update { it.copy(filter = filter) }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val app = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    InventoryViewModel(InventoryProvider.sourceFor(app)) as T
            }
        }
    }
}
