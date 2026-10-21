package com.sovereignops.updateauditor.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sovereignops.updateauditor.audit.InventoryProvider
import com.sovereignops.updateauditor.audit.InventorySource
import com.sovereignops.updateauditor.model.PackageState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Reads one package for the detail screen, off the main thread, once per screen. */
class DetailViewModel(
    private val source: InventorySource,
    private val packageName: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Loaded(val state: PackageState) : UiState
        data object NotFound : UiState
        data class Failed(val errorType: String) : UiState
    }

    private val mutableState = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val outcome = withContext(ioDispatcher) {
                try {
                    Result.success(source.readPackage(packageName))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Result.failure(failure)
                }
            }
            mutableState.value = outcome.fold(
                onSuccess = { found -> if (found == null) UiState.NotFound else UiState.Loaded(found) },
                onFailure = { UiState.Failed(it.javaClass.simpleName) },
            )
        }
    }

    companion object {
        fun factory(context: Context, packageName: String): ViewModelProvider.Factory {
            val app = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DetailViewModel(InventoryProvider.sourceFor(app), packageName) as T
            }
        }
    }
}
