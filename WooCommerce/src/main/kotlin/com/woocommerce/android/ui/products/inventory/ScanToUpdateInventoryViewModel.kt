package com.woocommerce.android.ui.products.inventory

import android.os.Parcelable
import android.text.Html
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
    private val _viewState: MutableStateFlow<ViewState> = savedState.getStateFlow(this, ViewState.Scanning, "viewState")
    val viewState: StateFlow<ViewState> = _viewState

    fun onBarcodeScanningResult(status: CodeScannerStatus) {
        if (viewState.value !is ViewState.Scanning) return

        if (status is CodeScannerStatus.Success) {
            _viewState.value = ViewState.Loading
            handleBarcodeScanningSuccess(status)
        }
    }

    fun onBottomSheetDismissed() {
        if (viewState.value is ViewState.Updating) return

        _viewState.value = ViewState.Scanning
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
                    _viewState.value = ViewState.Result(productInfo)
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
        _viewState.value = ViewState.Scanning
    }

    private fun triggerProductNotStockManagedSnackBar(product: Product) {
        val message = resourceProvider.getString(R.string.scan_to_update_inventory_product_not_stock_managed, product.sku)
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message)))
    }

    private suspend fun handleProductNotFound(barcode: String) {
        triggerProductNotFoundSnackBar(barcode)
        delay(SCANNER_RESTART_DEBOUNCE_MS)
        _viewState.value = ViewState.Scanning
    }

    private fun triggerProductNotFoundSnackBar(barcode: String) {
        val message = resourceProvider.getString(R.string.scan_to_update_inventory_unable_to_find_product, barcode)
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message)))
    }

    fun onIncrementQuantityClicked() {
        val state = viewState.value
        if (state !is ViewState.Result) return
        val product = productRepository.getProduct(state.product.id)
        _viewState.value = ViewState.Updating
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
        val oldQuantity = oldProduct.stockQuantity.toInt()
        val newQuantity = updatedProduct.stockQuantity.toInt()
        val quantityChangeString = Html.fromHtml("<font color=\"#3C3C4399\">$oldQuantity</font> → <font color=\"#00A32A\">$newQuantity</font>", Html.FROM_HTML_MODE_LEGACY)
        val message = resourceProvider.getString(R.string.scan_to_update_inventory_success_snackbar, quantityChangeString)
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message, true)))
        delay(SCANNER_RESTART_DEBOUNCE_MS)
        _viewState.value = ViewState.Scanning
    }

    private fun handleQuantityUpdateError() {
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringRes(R.string.scan_to_update_inventory_failure_snackbar)))
        _viewState.value = ViewState.Scanning
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
        object Scanning : ViewState()
        object Loading : ViewState()
        data class Result(val product: ProductInfo) : ViewState()
        object Updating : ViewState()
    }

    companion object {
        private const val SCANNER_RESTART_DEBOUNCE_MS = 1000L
    }
}
