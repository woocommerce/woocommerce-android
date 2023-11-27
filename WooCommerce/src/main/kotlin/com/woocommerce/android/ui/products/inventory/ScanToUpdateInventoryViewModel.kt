package com.woocommerce.android.ui.products.inventory

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.products.ProductDetailRepository
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
    private val productRepository: ProductDetailRepository,
) : ScopedViewModel(savedState) {
    private val _viewState: MutableStateFlow<ViewState> =
        savedState.getStateFlow(this, ViewState.BarcodeScanning, "viewState")
    val viewState: StateFlow<ViewState> = _viewState

    fun onBarcodeScanningResult(status: CodeScannerStatus) {
        if (viewState.value !is ViewState.BarcodeScanning) return

        if (status is CodeScannerStatus.Success) {
            _viewState.value = ViewState.ProductLoading
            handleBarcodeScanningSuccess(status)
        }
    }

    fun onBottomSheetDismissed() {
        if (viewState.value is ViewState.ProductUpdating) return

        _viewState.value = ViewState.BarcodeScanning
    }

    private fun handleBarcodeScanningSuccess(status: CodeScannerStatus.Success) = launch {
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringRes(R.string.scan_to_update_inventory_loading_product)))
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
                if (product.isStockManaged) {
                    _viewState.value = ViewState.ProductLoaded(productInfo)
                } else {
                    handleProductIsNotStockManaged(product)
                }
            } else {
                handleProductNotFound(status.code)
            }
        } else {
            handleProductNotFound(status.code)
        }
    }

    private suspend fun handleProductIsNotStockManaged(product: Product) {
        triggerProductNotStockManagedSnackBar(product)
        delay(SCANNER_RESTART_DEBOUNCE_MS)
        _viewState.value = ViewState.BarcodeScanning
    }

    private fun triggerProductNotStockManagedSnackBar(product: Product) {
        val message = resourceProvider.getString(
            R.string.scan_to_update_inventory_product_not_stock_managed,
            product.sku
        )
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message)))
    }

    private suspend fun handleProductNotFound(barcode: String) {
        triggerProductNotFoundSnackBar(barcode)
        delay(SCANNER_RESTART_DEBOUNCE_MS)
        _viewState.value = ViewState.BarcodeScanning
    }

    private fun triggerProductNotFoundSnackBar(barcode: String) {
        val message = resourceProvider.getString(R.string.scan_to_update_inventory_unable_to_find_product, barcode)
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message)))
    }

    fun onIncrementQuantityClicked() {
        val state = viewState.value
        if (state !is ViewState.ProductLoaded) return
        val product = productRepository.getProduct(state.product.id)
        _viewState.value = ViewState.ProductUpdating
        if (product == null) {
            handleQuantityUpdateError()
        } else {
            launch {
                val updatedProduct = product.copy(stockQuantity = product.stockQuantity + 1)
                val result = productRepository.updateProduct(updatedProduct)
                if (result) {
                    handleQuantityUpdateSuccess(product, updatedProduct)
                } else {
                    handleQuantityUpdateError()
                }
            }
        }
    }

    private suspend fun handleQuantityUpdateSuccess(oldProduct: Product, updatedProduct: Product) {
        val oldQuantity = oldProduct.stockQuantity
        val newQuantity = updatedProduct.stockQuantity
        val quantityChangeString = "$oldQuantity ➡ $newQuantity"
        val message = resourceProvider.getString(
            R.string.scan_to_update_inventory_success_snackbar,
            quantityChangeString
        )
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message)))
        delay(SCANNER_RESTART_DEBOUNCE_MS)
        _viewState.value = ViewState.BarcodeScanning
    }

    private fun handleQuantityUpdateError() {
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringRes(R.string.scan_to_update_inventory_failure_snackbar)))
        _viewState.value = ViewState.BarcodeScanning
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
        object BarcodeScanning : ViewState()
        object ProductLoading : ViewState()
        data class ProductLoaded(val product: ProductInfo) : ViewState()
        object ProductUpdating : ViewState()
    }

    companion object {
        private const val SCANNER_RESTART_DEBOUNCE_MS = 1000L
    }
}
