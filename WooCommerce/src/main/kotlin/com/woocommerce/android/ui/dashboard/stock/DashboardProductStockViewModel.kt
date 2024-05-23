package com.woocommerce.android.ui.dashboard.stock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.reviews.DashboardReviewsViewModel
import com.woocommerce.android.ui.dashboard.stock.DashboardProductStockViewModel.ViewState.Loading
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductStockStatus.LowStock
import com.woocommerce.android.ui.products.ProductStockStatus.OnBackorder
import com.woocommerce.android.ui.products.ProductStockStatus.OutOfStock
import com.woocommerce.android.ui.products.stock.ProductStockItem
import com.woocommerce.android.ui.products.stock.ProductStockRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart


@HiltViewModel(assistedFactory = DashboardReviewsViewModel.Factory::class)
class DashboardProductStockViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val productStockRepository: ProductStockRepository,
) : ScopedViewModel(savedStateHandle) {
    companion object {
        val supportedFilters = listOf(
            LowStock,
            OutOfStock,
            OnBackorder,
        )
    }

    private val _refreshTrigger = MutableSharedFlow<DashboardViewModel.RefreshEvent>(extraBufferCapacity = 1)
    private val refreshTrigger = merge(parentViewModel.refreshTrigger, _refreshTrigger)
        .onStart { emit(DashboardViewModel.RefreshEvent()) }

    val productStockState = savedStateHandle
        .getStateFlow(scope = viewModelScope, initialValue = Loading(LowStock))
        .asLiveData()

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
