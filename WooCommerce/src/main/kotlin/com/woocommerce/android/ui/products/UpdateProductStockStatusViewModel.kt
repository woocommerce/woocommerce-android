package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class UpdateProductStockStatusViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productListRepository: ProductListRepository,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {

    companion object {
        val AVAILABLE_STOCK_STATUSES = listOf(
            ProductStockStatus.InStock,
            ProductStockStatus.OutOfStock,
            ProductStockStatus.OnBackorder
        )
    }

    private val navArgs: UpdateProductStockStatusFragmentArgs by savedStateHandle.navArgs()

    private val stockStatusUiState = savedStateHandle.getStateFlow(
        viewModelScope,
        UpdateStockStatusUiState()
    )

    val viewState = stockStatusUiState.asLiveData()

    init {
        loadProductStockStatuses(navArgs.selectedProductIds.toList())
    }

    fun setCurrentStockStatus(newStatus: ProductStockStatus) {
        stockStatusUiState.update { currentState ->
            currentState.copy(currentProductStockStatus = newStatus)
        }
    }

    fun updateStockStatusForProducts() {
        analyticsTracker.track(AnalyticsEvent.PRODUCT_STOCK_STATUSES_UPDATE_DONE_TAPPED)

        viewModelScope.launch {
            val currentStatus = stockStatusUiState.value.currentProductStockStatus
            stockStatusUiState.update { currentState ->
                currentState.copy(isProgressDialogVisible = true)
            }
            val result = productListRepository.bulkUpdateStockStatus(navArgs.selectedProductIds.toList(), currentStatus)

            stockStatusUiState.update { currentState ->
                currentState.copy(isProgressDialogVisible = false)
            }

            when (result) {
                is UpdateStockStatusResult.Updated -> {
                    val snackText = R.string.product_update_stock_status_completed
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(snackText))
                    triggerEvent(MultiLiveEvent.Event.ExitWithResult(UpdateStockStatusExitState.Success))
                }
                is UpdateStockStatusResult.Error -> {
                    val snackText = R.string.product_update_stock_status_error
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(snackText))
                    triggerEvent(MultiLiveEvent.Event.ExitWithResult(UpdateStockStatusExitState.Error))
                }
                is UpdateStockStatusResult.IsManagedProducts -> {
                    val snackText = R.string.product_update_stock_status_managed_products
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(snackText))
                    triggerEvent(MultiLiveEvent.Event.ExitWithResult(UpdateStockStatusExitState.Error))
                }
            }
        }
    }

    private fun loadProductStockStatuses(productIds: List<Long>) {
        viewModelScope.launch {
            val stockStatusInfos = productListRepository.fetchStockStatuses(productIds)
            val distinctStatuses = stockStatusInfos.map { it.stockStatus }.distinct()
            val productsToUpdateCount = stockStatusInfos.count { !it.manageStock }
            val ignoredProductsCount = stockStatusInfos.size - productsToUpdateCount

            val stockStatusState = if (distinctStatuses.size > 1) {
                StockStatusState.Mixed
            } else {
                StockStatusState.Common(distinctStatuses.firstOrNull() ?: ProductStockStatus.InStock)
            }

            stockStatusUiState.update {
                it.copy(
                    productsToUpdateCount = productsToUpdateCount,
                    ignoredProductsCount = ignoredProductsCount,
                    currentStockStatusState = stockStatusState
                )
            }
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.ExitWithResult(UpdateStockStatusExitState.NoChange))
    }

    @Parcelize
    data class UpdateStockStatusUiState(
        val productsToUpdateCount: Int = 0,
        val ignoredProductsCount: Int = 0,
        val isProgressDialogVisible: Boolean = false,
        val currentProductStockStatus: ProductStockStatus = ProductStockStatus.InStock,
        val stockStockStatuses: List<ProductStockStatus> = AVAILABLE_STOCK_STATUSES,
        val currentStockStatusState: StockStatusState = StockStatusState.Mixed
    ) : Parcelable

    @Parcelize
    data class ProductStockStatusInfo(
        val productId: Long,
        val stockStatus: ProductStockStatus,
        val manageStock: Boolean
    ) : Parcelable

    sealed class StockStatusState : Parcelable {
        @Parcelize
        data object Mixed : StockStatusState()

        @Parcelize
        data class Common(val status: ProductStockStatus) : StockStatusState()
    }

    sealed class UpdateStockStatusResult {
        data object Updated : UpdateStockStatusResult()
        data object Error : UpdateStockStatusResult()
        data object IsManagedProducts : UpdateStockStatusResult()
    }

    @Parcelize
    enum class UpdateStockStatusExitState : Parcelable {
        Success,
        Error,
        NoChange
    }
}
