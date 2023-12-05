package com.woocommerce.android.ui.products.inventory

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
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
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

@HiltViewModel
class ScanToUpdateInventoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val fetchProductBySKU: FetchProductBySKU,
    private val resourceProvider: ResourceProvider,
    private val productRepository: ProductDetailRepository,
    private val variationRepository: VariationDetailRepository,
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
                    triggerEvent(OpenInventoryUpdateBottomSheet)
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
        val message = resourceProvider.getString(
            R.string.scan_to_update_inventory_unable_to_find_product,
            barcode
        )
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message)))
    }

    fun onIncrementQuantityClicked() {
        val state = viewState.value
        if (state !is ViewState.ProductLoaded) return
        updateQuantity(state.product.copy(quantity = state.product.quantity + 1))
    }

    fun onUpdateQuantityClicked() {
        val state = viewState.value
        if (state !is ViewState.ProductLoaded) return
        updateQuantity(state.product)
    }

    private fun updateQuantity(updatedProductInfo: ProductInfo) {
        val product = productRepository.getProduct(updatedProductInfo.id)
        _viewState.value = ViewState.ProductUpdating
        if (product == null) {
            handleQuantityUpdateError()
        } else {
            launch {
                val result = if (product.isVariable()) {
                    product.updateVariation(updatedProductInfo)
                } else {
                    product.updateProduct(updatedProductInfo)
                }
                if (result.isSuccess) {
                    handleQuantityUpdateSuccess(
                        product.stockQuantity.toInt().toString(),
                        updatedProductInfo.quantity.toString()
                    )
                } else {
                    handleQuantityUpdateError()
                }
            }
        }
    }

    private suspend fun Product.updateProduct(updatedProductInfo: ProductInfo): Result<Unit> {
        val updatedProduct = copy(stockQuantity = updatedProductInfo.quantity.toDouble())
        val result: Boolean = productRepository.updateProduct(updatedProduct)
        return if (result) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Unable to update product"))
        }
    }

    private suspend fun Product.updateVariation(updatedProductInfo: ProductInfo): Result<Unit> {
        val variation: ProductVariation? = variationRepository.getVariation(
            remoteProductId = parentId,
            remoteVariationId = remoteId
        )
        val updatedVariation = variation?.copy(stockQuantity = updatedProductInfo.quantity.toDouble())
            ?: return Result.failure(Exception("Unable to find variation"))

        val result: WCProductStore.OnVariationUpdated = variationRepository.updateVariation(updatedVariation)
        return if (result.isError) {
            Result.failure(Exception("Unable to update variation"))
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun handleQuantityUpdateSuccess(oldQuantity: String, updatedQuantity: String) {
        val quantityChangeString = "$oldQuantity ➡ $updatedQuantity"
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

    fun onManualQuantityEntered(newQuantity: String) {
        val state = viewState.value
        if (state !is ViewState.ProductLoaded) return
        _viewState.value = state.copy(
            product = state.product.copy(quantity = newQuantity.toIntOrNull() ?: 0),
            isPendingUpdate = true
        )
    }

    private fun Product.isVariable(): Boolean {
        return this.parentId != 0L
    }

    fun onViewProductDetailsClicked() {
        val state = viewState.value
        if (state !is ViewState.ProductLoaded) return
        triggerEvent(NavigateToProductDetailsEvent(state.product.id))
    }

    @Parcelize
    data class ProductInfo(
        val id: Long,
        val name: String,
        val imageUrl: String,
        val sku: String,
        val quantity: Int,
        val isPendingUpdate: Boolean = false,
    ) : Parcelable

    @Parcelize
    sealed class ViewState : Parcelable {
        object BarcodeScanning : ViewState()
        object ProductLoading : ViewState()
        data class ProductLoaded(
            val product: ProductInfo,
            val isPendingUpdate: Boolean = false,
            val originalQuantity: String = product.quantity.toString(),
        ) : ViewState()

        object ProductUpdating : ViewState()
    }

    data class NavigateToProductDetailsEvent(val productId: Long) : Event()
    object OpenInventoryUpdateBottomSheet : Event()

    companion object {
        private const val SCANNER_RESTART_DEBOUNCE_MS = 3500L
    }
}
