package com.woocommerce.android.ui.dashboard.stock

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.stock.DashboardProductStockViewModel.ViewState.Loading
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.stock.ProductStockItem
import com.woocommerce.android.ui.products.stock.ProductStockRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = DashboardProductStockViewModel.Factory::class)
class DashboardProductStockViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val productStockRepository: ProductStockRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
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
        .onStart {
            if (productStockState.value !is ViewState.Success) {
                emit(DashboardViewModel.RefreshEvent()) // Avoid refreshing when stock items are already loaded
            }
        }
    private val status = savedStateHandle.getStateFlow<ProductStockStatus>(viewModelScope, ProductStockStatus.LowStock)

    val productStockState: LiveData<ViewState> = status
        .flatMapLatest {
            refreshTrigger.map { refresh -> Pair(refresh, it) }
        }
        .transformLatest { (refresh, status) ->
            emit(Loading(status))
            productStockRepository.fetchProductStockReport(status, refresh.isForced)
                .fold(
                    onSuccess = {
                        val sortedProductStockItems = it.sortedBy { item -> item.stockQuantity }
                        emit(ViewState.Success(sortedProductStockItems, status))
                    },
                    onFailure = {
                        when ((it as? WooException)?.error?.type) {
                            WooErrorType.API_NOT_FOUND -> emit(ViewState.Error.WCAnalyticsDisabled)
                            else -> emit(ViewState.Error.Generic)
                        }
                    }
                )
        }.asLiveData()

    fun onFilterSelected(productStockStatus: ProductStockStatus) {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.STOCK.trackingIdentifier)
        this.status.value = productStockStatus
    }

    fun onRetryClicked() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_TYPE to DashboardWidget.Type.STOCK.trackingIdentifier
            )
        )
        _refreshTrigger.tryEmit(DashboardViewModel.RefreshEvent())
    }

    fun onProductClicked(product: ProductStockItem) {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.STOCK.trackingIdentifier)
        val id = when {
            product.parentProductId != 0L -> product.parentProductId
            else -> product.productId
        }
        triggerEvent(OpenProductDetail(id))
    }

    sealed interface ViewState {
        data class Loading(val selectedFilter: ProductStockStatus) : ViewState
        data class Success(
            val productStockItems: List<ProductStockItem>,
            val selectedFilter: ProductStockStatus
        ) : ViewState

        enum class Error : ViewState {
            Generic, WCAnalyticsDisabled
        }
    }

    data class OpenProductDetail(val productId: Long) : MultiLiveEvent.Event()

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardProductStockViewModel
    }
}
