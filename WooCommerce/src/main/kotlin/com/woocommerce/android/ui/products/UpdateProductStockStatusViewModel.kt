package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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
    private val productListRepository: ProductListRepository
) : ScopedViewModel(savedStateHandle) {

    private val navArgs: UpdateProductStockStatusFragmentArgs by savedStateHandle.navArgs()

    private val stockStatusUiState = savedStateHandle.getStateFlow(
        viewModelScope,
        UpdateStockStatusUiState()
    )

    val viewState = stockStatusUiState.asLiveData()

    init {
        loadProductStockStatuses(navArgs.selectedProductIds.toList())
    }

    fun updateStockStatusForProducts(@Suppress("UNUSED_PARAMETER") newStatus: ProductStockStatus) {
        viewModelScope.launch {
//            val result = productListRepository.bulkUpdateStockStatus(navArgs.selectedProductIds.toList(), newStatus)
//            stockStatusUiState.update { currentState ->
//              //  currentState.copy(updateResult = result)
//            }
        }
    }

    private fun loadProductStockStatuses(productIds: List<Long>) {
        viewModelScope.launch {
            val stockStatusInfos = productListRepository.fetchStockStatuses(productIds)
            val distinctStatuses = stockStatusInfos.map { it.stockStatus }.distinct()
            val productsToUpdateCount = stockStatusInfos.count { !it.manageStock }
            val ignoredProductsCount = stockStatusInfos.size - productsToUpdateCount

            val statusFrequency = stockStatusInfos.groupingBy { it.stockStatus }.eachCount()
            val mostFrequentStatus = statusFrequency.maxByOrNull { it.value }?.key ?: ProductStockStatus.InStock

            val stockStatusState = if (distinctStatuses.size > 1) {
                StockStatusState.Mixed
            } else {
                StockStatusState.Common(distinctStatuses.firstOrNull() ?: ProductStockStatus.InStock)
            }

            stockStatusUiState.update {
                it.copy(
                    productsToUpdateCount = productsToUpdateCount,
                    ignoredProductsCount = ignoredProductsCount,
                    initialProductStockStatus = mostFrequentStatus,
                    currentStockStatusState = stockStatusState
                )
            }
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    @Parcelize
    data class UpdateStockStatusUiState(
        val productsToUpdateCount: Int = 0,
        val ignoredProductsCount: Int = 0,
        val initialProductStockStatus: ProductStockStatus = ProductStockStatus.InStock,
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
}
