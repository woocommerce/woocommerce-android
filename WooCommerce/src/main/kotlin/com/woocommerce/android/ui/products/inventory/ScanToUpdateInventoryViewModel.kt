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
import com.woocommerce.android.viewmodel.MultiLiveEvent
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
        savedState.getStateFlow(this, ViewState.QuickInventoryBottomSheetHidden, "viewState")
    val viewState: StateFlow<ViewState> = _viewState

    private val scanToUpdateInventoryState: MutableStateFlow<ScanToUpdateInventoryState> =
        savedState.getStateFlow(this, ScanToUpdateInventoryState.Idle, "productSearchState")

    fun onBarcodeScanningResult(status: CodeScannerStatus) {
        if (scanToUpdateInventoryState.value != ScanToUpdateInventoryState.Idle) return

        if (status is CodeScannerStatus.Success) {
            scanToUpdateInventoryState.value = ScanToUpdateInventoryState.FetchingProduct
            handleBarcodeScanningSuccess(status)
        }
    }

    fun onBottomSheetDismissed() {
        if (scanToUpdateInventoryState.value == ScanToUpdateInventoryState.UpdatingProduct) return
        scanToUpdateInventoryState.value = ScanToUpdateInventoryState.Idle
        _viewState.value = ViewState.QuickInventoryBottomSheetHidden
    }

    private fun handleBarcodeScanningSuccess(status: CodeScannerStatus.Success) = launch {
        _viewState.value = ViewState.Loading
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
                    _viewState.value = ViewState.QuickInventoryBottomSheetVisible(productInfo)
                } else {
                    handleProductIsNotStockManaged(product)
                    _viewState.value = ViewState.QuickInventoryBottomSheetHidden
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
        scanToUpdateInventoryState.value = ScanToUpdateInventoryState.Idle
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
        scanToUpdateInventoryState.value = ScanToUpdateInventoryState.Idle
    }

    private fun triggerProductNotFoundSnackBar(barcode: String) {
        val message = resourceProvider.getString(R.string.scan_to_update_inventory_unable_to_find_product, barcode)
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message)))
    }

    fun onIncrementQuantityClicked() {
        val state = viewState.value
        if (state !is ViewState.QuickInventoryBottomSheetVisible) return
        updateQuantity(state.product.copy(quantity = state.product.quantity + 1))
    }

    fun onUpdateQuantityClicked() {
        val state = viewState.value
        if (state !is ViewState.QuickInventoryBottomSheetVisible) return
        // if user input is empty or invalid, do nothing
        val newQuantityValue = state.newQuantity.toIntOrNull() ?: return
        updateQuantity(state.product.copy(quantity = newQuantityValue))
    }

    private fun updateQuantity(updatedProductInfo: ProductInfo) = launch {
        _viewState.value = ViewState.Loading
        scanToUpdateInventoryState.value = ScanToUpdateInventoryState.UpdatingProduct
        val product = productRepository.getProduct(updatedProductInfo.id)
        if (product == null) {
            handleQuantityUpdateError()
            _viewState.value = ViewState.QuickInventoryBottomSheetHidden
            scanToUpdateInventoryState.value = ScanToUpdateInventoryState.Idle
        } else {
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
            _viewState.value = ViewState.QuickInventoryBottomSheetHidden
            delay(SCANNER_RESTART_DEBOUNCE_MS)
            scanToUpdateInventoryState.value = ScanToUpdateInventoryState.Idle
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

    private fun handleQuantityUpdateSuccess(oldQuantity: String, updatedQuantity: String) {
        val quantityChangeString = "$oldQuantity ➡ $updatedQuantity"
        val message = resourceProvider.getString(
            R.string.scan_to_update_inventory_success_snackbar,
            quantityChangeString
        )
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message)))
    }

    private fun handleQuantityUpdateError() {
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringRes(R.string.scan_to_update_inventory_failure_snackbar)))
    }

    fun onManualQuantityEntered(newQuantity: String) {
        val state = viewState.value
        if (state !is ViewState.QuickInventoryBottomSheetVisible) return
        try {
            val quantity = if (newQuantity.isNotBlank() && newQuantity.isNotEmpty()) {
                // ignore negative values
                if (newQuantity.toInt() < 0) {
                    state.newQuantity
                } else {
                    newQuantity
                }
            } else {
                newQuantity
            }
            _viewState.value = state.copy(newQuantity = quantity, isPendingUpdate = true)
        } catch (_: NumberFormatException) {}
    }

    private fun Product.isVariable(): Boolean {
        return this.parentId != 0L
    }

    fun onViewProductDetailsClicked() {
        val state = viewState.value
        if (state !is ViewState.QuickInventoryBottomSheetVisible) return
        triggerEvent(NavigateToProductDetailsEvent(state.product.id))
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
        data class QuickInventoryBottomSheetVisible(
            val product: ProductInfo,
            val isPendingUpdate: Boolean = false,
            val originalQuantity: String = product.quantity.toString(),
            val newQuantity: String = product.quantity.toString()
        ) : ViewState()
        object QuickInventoryBottomSheetHidden : ViewState()
        object Loading : ViewState()
    }

    enum class ScanToUpdateInventoryState {
        Idle, FetchingProduct, UpdatingProduct
    }

    data class NavigateToProductDetailsEvent(val productId: Long) : MultiLiveEvent.Event()

    companion object {
        private const val SCANNER_RESTART_DEBOUNCE_MS = 3500L
    }
}
