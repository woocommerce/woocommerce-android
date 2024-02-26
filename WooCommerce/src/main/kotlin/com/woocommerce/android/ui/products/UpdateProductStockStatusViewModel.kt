package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    fun updateStockStatusForProducts(newStatus: ProductStockStatus, productIds: List<Long>) {
        viewModelScope.launch {
            val result = productListRepository.bulkUpdateStockStatus(productIds, newStatus)
            stockStatusUiState.update { currentState ->
                currentState.copy(updateResult = result)
            }
        }
    }

    fun loadProductStockStatuses(productIds: List<Long>) {
        viewModelScope.launch {
            val stockStatusInfos = productListRepository.fetchStockStatuses(productIds)
            val distinctStatuses = stockStatusInfos.map { it.stockStatus }.distinct()
            val isMixedStatus = distinctStatuses.size > 1
            val productsToUpdateCount = stockStatusInfos.count { !it.manageStock }
            val ignoredProductsCount = stockStatusInfos.size - productsToUpdateCount

            val statusFrequency = stockStatusInfos.groupingBy { it.stockStatus }.eachCount()
            val mostFrequentStatus = statusFrequency.maxByOrNull { it.value }?.key ?: ProductStockStatus.InStock


            stockStatusUiState.update {
                it.copy(
                    stockStatusInfos = stockStatusInfos,
                    isMixedStatus = isMixedStatus,
                    productsToUpdateCount = productsToUpdateCount,
                    ignoredProductsCount = ignoredProductsCount,
                    initialProductStockStatus = mostFrequentStatus
                )
            }
        }
    }

    data class UpdateStockStatusUiState(
        val stockStatusInfos: List<ProductStockStatusInfo> = emptyList(),
        val isMixedStatus: Boolean = false,
        val productsToUpdateCount: Int = 0,
        val ignoredProductsCount: Int = 0,
        val updateResult: RequestResult? = null,
        val initialProductStockStatus: ProductStockStatus = ProductStockStatus.InStock
    )

    data class ProductStockStatusInfo(
        val productId: Long,
        val stockStatus: ProductStockStatus,
        val manageStock: Boolean
    )
}
