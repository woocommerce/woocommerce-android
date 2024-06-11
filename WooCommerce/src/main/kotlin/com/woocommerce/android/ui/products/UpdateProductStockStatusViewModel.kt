package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
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
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val resourceProvider: ResourceProvider,
) : ScopedViewModel(savedStateHandle) {

    companion object {
        val AVAILABLE_STOCK_STATUSES = listOf(
            ProductStockStatus.InStock,
            ProductStockStatus.OutOfStock,
            ProductStockStatus.OnBackorder
        )

        private fun buildStatusMessage(
            productsToUpdateCount: Int,
            managedProductsIgnoredCount: Int,
            variableProductsIgnoredCount: Int,
            resourceProvider: ResourceProvider
        ): String {
            return buildString {
                val updateMessage = if (productsToUpdateCount == 1) {
                    resourceProvider.getString(R.string.product_update_stock_status_update_count_singular)
                } else {
                    resourceProvider.getString(R.string.product_update_stock_status_update_count, productsToUpdateCount)
                }
                append(updateMessage)

                if (managedProductsIgnoredCount > 0) {
                    append(" ")
                    val ignoreMessage = if (managedProductsIgnoredCount == 1) {
                        resourceProvider.getString(R.string.product_update_stock_status_ignored_count_singular)
                    } else {
                        resourceProvider.getString(
                            R.string.product_update_stock_status_ignored_count,
                            managedProductsIgnoredCount
                        )
                    }
                    append(ignoreMessage)
                }

                if (variableProductsIgnoredCount > 0) {
                    append(" ")
                    val variableIgnoreMessage = if (variableProductsIgnoredCount == 1) {
                        resourceProvider.getString(R.string.product_update_stock_status_variable_ignored_count_singular)
                    } else {
                        resourceProvider.getString(
                            R.string.product_update_stock_status_variable_ignored_count,
                            variableProductsIgnoredCount
                        )
                    }
                    append(variableIgnoreMessage)
                }
            }
        }
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

    fun onStockStatusSelected(newStatus: ProductStockStatus) {
        stockStatusUiState.update { currentState ->
            currentState.copy(currentProductStockStatus = newStatus)
        }
    }

    fun onDoneButtonClicked() {
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

                is UpdateStockStatusResult.IsVariableProducts -> {
                    val snackText = R.string.product_update_stock_status_variable_products
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

            val productsToUpdateCount = stockStatusInfos.count { !it.manageStock && !it.isVariable }
            val managedProductsIgnoredCount = stockStatusInfos.count { it.manageStock }
            val variableProductsIgnoredCount = stockStatusInfos.count { it.isVariable }

            val statusMessage = buildStatusMessage(
                productsToUpdateCount,
                managedProductsIgnoredCount,
                variableProductsIgnoredCount,
                resourceProvider
            )

            val stockStatusState = if (distinctStatuses.size > 1) {
                StockStatusState.Mixed
            } else {
                StockStatusState.Common(distinctStatuses.firstOrNull() ?: ProductStockStatus.InStock)
            }

            val commonProductStockStatus = if (distinctStatuses.size == 1) {
                distinctStatuses.first()
            } else {
                ProductStockStatus.InStock
            }

            stockStatusUiState.update {
                it.copy(
                    statusMessage = statusMessage,
                    currentProductStockStatus = commonProductStockStatus,
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
        val statusMessage: String = "",
        val isProgressDialogVisible: Boolean = false,
        val currentProductStockStatus: ProductStockStatus = ProductStockStatus.InStock,
        val stockStockStatuses: List<ProductStockStatus> = AVAILABLE_STOCK_STATUSES,
        val currentStockStatusState: StockStatusState = StockStatusState.Mixed
    ) : Parcelable

    @Parcelize
    data class ProductStockStatusInfo(
        val productId: Long,
        val stockStatus: ProductStockStatus,
        val manageStock: Boolean,
        val isVariable: Boolean
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
        data object IsVariableProducts : UpdateStockStatusResult()
    }

    @Parcelize
    enum class UpdateStockStatusExitState : Parcelable {
        Success,
        Error,
        NoChange
    }
}
