package com.woocommerce.android.ui.dashboard.stock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.stock.DashboardProductStockViewModel.ViewState.Loading
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.stock.ProductStockItem
import com.woocommerce.android.ui.products.stock.ProductStockRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = DashboardProductStockViewModel.Factory::class)
class DashboardProductStockViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val productStockRepository: ProductStockRepository,
) : ScopedViewModel(savedStateHandle) {
    companion object {
        val supportedFilters = listOf(
            ProductStockStatus.LowStock,
            ProductStockStatus.OutOfStock,
            ProductStockStatus.OnBackorder,
        )
    }

    private val _refreshTrigger = MutableSharedFlow<DashboardViewModel.RefreshEvent>(extraBufferCapacity = 1)
    private val refreshTrigger = merge(parentViewModel.refreshTrigger, _refreshTrigger)
        .onStart { emit(DashboardViewModel.RefreshEvent()) }
    private val status = savedStateHandle.getStateFlow<ProductStockStatus>(viewModelScope, ProductStockStatus.LowStock)

    val productStockState = status
        .flatMapLatest {
            refreshTrigger.map { refresh -> Pair(refresh, it) }
        }
        .transformLatest { (_, status) ->
            emit(Loading(status))
            productStockRepository.fetchProductStockReport(status)
                .fold(
                    onSuccess = { emit(ViewState.Success(it, status)) },
                    onFailure = { emit(ViewState.Error) }
                )
        }.asLiveData()

    fun onFilterSelected(productStockStatus: ProductStockStatus) {
        this.status.value = productStockStatus
    }

    sealed interface ViewState {
        data class Loading(val selectedFilter: ProductStockStatus) : ViewState
        data class Success(
            val productStockItems: List<ProductStockItem>,
            val selectedFilter: ProductStockStatus
        ) : ViewState

        data object Error : ViewState
    }

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardProductStockViewModel
    }
}
