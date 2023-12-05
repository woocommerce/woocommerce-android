package com.woocommerce.android.ui.products.inventory

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ScanToUpdateInventoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val fetchProductBySKU: FetchProductBySKU,
    private val resourceProvider: ResourceProvider,
) : ScopedViewModel(savedState) {
    private val _viewState: MutableStateFlow<ViewState> =
        savedState.getStateFlow(this, ViewState.QuickInventoryBottomSheetHidden, "viewState")
    val viewState: StateFlow<ViewState> = _viewState

    private val productSearchState: MutableStateFlow<ProductSearchState> =
        savedState.getStateFlow(this, ProductSearchState.Idle, "productSearchState")

    fun onBarcodeScanningResult(status: CodeScannerStatus) {
        if (productSearchState.value != ProductSearchState.Idle) return

        if (status is CodeScannerStatus.Success) {
            productSearchState.value = ProductSearchState.Ongoing
            handleBarcodeScanningSuccess(status)
        }
    }

    fun onBottomSheetDismissed() {
        productSearchState.value = ProductSearchState.Idle
    }

    private fun handleBarcodeScanningSuccess(status: CodeScannerStatus.Success) = launch {
        val productResult: Result<Product> = fetchProductBySKU(status.code, status.format)
        if (productResult.isSuccess) {
            val product = productResult.getOrNull()
            if (product != null) {
                val productInfo = ProductInfo(
                    id = product.remoteId,
                    name = product.name,
                    imageUrl = product.firstImageUrl.orEmpty(),
                    sku = product.sku,
                    quantity = product.stockQuantity.toInt(),
                )
                _viewState.value = ViewState.QuickInventoryBottomSheetVisible(productInfo)
            } else {
                handleProductNotFound(status.code)
            }
        } else {
            handleProductNotFound(status.code)
        }
    }

    private suspend fun handleProductNotFound(barcode: String) {
        triggerProductNotFoundSnackBar(barcode)
        delay(SCANNER_RESTART_DEBOUNCE_MS)
        productSearchState.value = ProductSearchState.Idle
    }

    private fun triggerProductNotFoundSnackBar(barcode: String) {
        val message = resourceProvider.getString(R.string.scan_to_update_inventory_unable_to_find_product, barcode)
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message)))
    }

    @Suppress("ForbiddenComment")
    fun onIncrementQuantityClicked() {
        // TODO: Implement actual logic
        productSearchState.value = ProductSearchState.Idle
    }

    @Parcelize
    data class ProductInfo(
        val id: Long,
        val name: String,
        val imageUrl: String,
        val sku: String,
        val quantity: Int,
    ) : Parcelable

    @Parcelize
    sealed class ViewState : Parcelable {
        data class QuickInventoryBottomSheetVisible(val product: ProductInfo) : ViewState()
        object QuickInventoryBottomSheetHidden : ViewState()
    }

    enum class ProductSearchState {
        Idle, Ongoing
    }

    companion object {
        private const val SCANNER_RESTART_DEBOUNCE_MS = 1000L
    }
}
