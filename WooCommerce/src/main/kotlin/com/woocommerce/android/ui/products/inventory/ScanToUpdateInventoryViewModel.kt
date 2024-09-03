package com.woocommerce.android.ui.products.inventory

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ITEM_STOCK_MANAGED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.products.details.ProductDetailRepository
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
    private val tracker: AnalyticsTrackerWrapper
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
        tracker.track(AnalyticsEvent.PRODUCT_QUICK_INVENTORY_UPDATE_DISMISSED)
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
                    isStockManaged = isItemStockManaged(product)
                )
                if (isItemStockManaged(product)) {
                    tracker.track(
                        AnalyticsEvent.PRODUCT_QUICK_INVENTORY_UPDATE_BOTTOM_SHEET_SHOWN,
                        mapOf(KEY_ITEM_STOCK_MANAGED to true)
                    )
                    _viewState.value = ViewState.QuickInventoryBottomSheetVisible(productInfo)
                } else {
                    handleProductIsNotStockManaged(product)
                }
            } else {
                handleProductNotFound(status.code)
            }

            tracker.track(
                AnalyticsEvent.PRODUCT_SEARCH_VIA_SKU_SUCCESS,
                mapOf(
                    KEY_SCANNING_SOURCE to AnalyticsTracker.SCAN_TO_UPDATE_INVENTORY
                )
            )
        } else {
            handleProductNotFound(status.code)
        }
    }

    private suspend fun isItemStockManaged(product: Product): Boolean =
        if (product.isVariation()) {
            variationRepository.getVariationOrNull(product.parentId, product.remoteId).let {
                it?.isStockManaged == true
            }
        } else {
            product.isStockManaged
        }

    private fun handleProductIsNotStockManaged(product: Product) {
        tracker.track(
            AnalyticsEvent.PRODUCT_QUICK_INVENTORY_UPDATE_BOTTOM_SHEET_SHOWN,
            mapOf(KEY_ITEM_STOCK_MANAGED to false)
        )
        _viewState.value = ViewState.QuickInventoryBottomSheetVisible(
            product = ProductInfo(
                id = product.remoteId,
                name = product.name,
                imageUrl = product.firstImageUrl.orEmpty(),
                sku = product.sku,
                quantity = product.stockQuantity.toInt(),
                isStockManaged = false
            )
        )
    }

    private suspend fun handleProductNotFound(barcode: String) {
        tracker.track(
            AnalyticsEvent.PRODUCT_SEARCH_VIA_SKU_FAILURE,
            mapOf(
                KEY_SCANNING_SOURCE to AnalyticsTracker.SCAN_TO_UPDATE_INVENTORY
            )
        )
        triggerProductNotFoundSnackBar(barcode)
        _viewState.value = ViewState.QuickInventoryBottomSheetHidden
        delay(SCANNER_RESTART_DEBOUNCE_MS)
        scanToUpdateInventoryState.value = ScanToUpdateInventoryState.Idle
    }

    private fun triggerProductNotFoundSnackBar(barcode: String) {
        val message = resourceProvider.getString(R.string.scan_to_update_inventory_unable_to_find_product, barcode)
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message)))
    }

    fun onIncrementQuantityClicked() {
        tracker.track(AnalyticsEvent.PRODUCT_QUICK_INVENTORY_UPDATE_INCREMENT_QUANTITY_TAPPED)
        val state = viewState.value
        if (state !is ViewState.QuickInventoryBottomSheetVisible) return
        updateQuantity(state.product.copy(quantity = state.product.quantity + 1), isUndoUpdate = false)
    }

    fun onUpdateQuantityClicked() {
        tracker.track(AnalyticsEvent.PRODUCT_QUICK_INVENTORY_UPDATE_MANUAL_QUANTITY_UPDATE_TAPPED)
        val state = viewState.value
        if (state !is ViewState.QuickInventoryBottomSheetVisible) return
        // if user input is empty or invalid, do nothing
        val newQuantityValue = state.newQuantity.toIntOrNull() ?: return
        updateQuantity(state.product.copy(quantity = newQuantityValue), isUndoUpdate = false)
    }

    private fun updateQuantity(
        updatedProductInfo: ProductInfo,
        isUndoUpdate: Boolean,
        onSuccess: () -> Unit = {},
        onError: () -> Unit = {},
        onUndoSuccess: () -> Unit = {}
    ) = launch {
        _viewState.value = ViewState.Loading
        scanToUpdateInventoryState.value = ScanToUpdateInventoryState.UpdatingProduct
        val product = productRepository.getProduct(updatedProductInfo.id)
        if (product == null) {
            handleQuantityUpdateError()
            _viewState.value = ViewState.QuickInventoryBottomSheetHidden
            scanToUpdateInventoryState.value = ScanToUpdateInventoryState.Idle
        } else {
            val result = if (product.isVariation()) {
                product.updateVariation(updatedProductInfo)
            } else {
                product.updateProduct(updatedProductInfo)
            }
            if (result.isSuccess) {
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_QUICK_INVENTORY_QUANTITY_UPDATE_SUCCESS)
                val oldQuantity = product.stockQuantity.toInt().toString()
                val updatedQuantity = updatedProductInfo.quantity.toString()
                if (!isUndoUpdate) {
                    showQuantityUpdateSuccessSnackbar(oldQuantity, updatedQuantity, updatedProductInfo)
                    onSuccess()
                } else {
                    onUndoSuccess()
                }
            } else {
                handleQuantityUpdateError()
                onError()
            }
            _viewState.value = ViewState.QuickInventoryBottomSheetHidden
            delay(SCANNER_RESTART_DEBOUNCE_MS)
            scanToUpdateInventoryState.value = ScanToUpdateInventoryState.Idle
        }
    }

    private suspend fun Product.updateProduct(updatedProductInfo: ProductInfo): Result<Unit> {
        val updatedProduct = copy(
            stockQuantity = updatedProductInfo.quantity.toDouble(),
            isStockManaged = updatedProductInfo.isStockManaged,
        )
        val result: Boolean = productRepository.updateProduct(updatedProduct).first
        return if (result) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Unable to update product"))
        }
    }

    private suspend fun Product.updateVariation(updatedProductInfo: ProductInfo): Result<Unit> {
        val variationId = remoteId
        val productId = parentId
        val variation: ProductVariation? = variationRepository.getVariationOrNull(
            remoteProductId = productId,
            remoteVariationId = variationId
        )
        val updatedVariation = variation?.copy(
            stockQuantity = updatedProductInfo.quantity.toDouble(),
            isStockManaged = updatedProductInfo.isStockManaged,
        ) ?: return Result.failure(Exception("Unable to find variation"))

        val result: WCProductStore.OnVariationUpdated = variationRepository.updateVariation(updatedVariation)
        return if (result.isError) {
            Result.failure(Exception("Unable to update variation"))
        } else {
            Result.success(Unit)
        }
    }

    private fun showQuantityUpdateSuccessSnackbar(
        oldQuantity: String,
        updatedQuantity: String,
        productInfo: ProductInfo
    ) {
        val quantityChangeString = "$oldQuantity ➡ $updatedQuantity"
        val message = resourceProvider.getString(
            R.string.scan_to_update_inventory_success_snackbar,
            quantityChangeString
        )
        triggerEvent(
            MultiLiveEvent.Event.ShowUndoSnackbar(
                message = message,
                undoAction = {
                    onUpdateQuantityUndo(oldQuantity, productInfo)
                },
            )
        )
    }

    private fun handleQuantityUpdateError() {
        AnalyticsTracker.track(AnalyticsEvent.PRODUCT_QUICK_INVENTORY_QUANTITY_UPDATE_FAILURE)
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringRes(R.string.scan_to_update_inventory_failure_snackbar)))
    }

    private fun onUpdateQuantityUndo(oldQuantity: String, productInfo: ProductInfo) {
        updateQuantity(
            productInfo.copy(quantity = oldQuantity.toInt()),
            isUndoUpdate = true,
            onUndoSuccess = {
                triggerEvent(
                    ShowUiStringSnackbar(
                        UiString.UiStringText(
                            resourceProvider.getString(R.string.scan_to_update_inventory_undo_snackbar)
                        )
                    )
                )
            },
            onError = {
                triggerEvent(
                    ShowUiStringSnackbar(UiString.UiStringRes(R.string.scan_to_update_inventory_failure_snackbar))
                )
            }
        )
    }

    fun onManualQuantityEntered(newQuantity: String) {
        val state = viewState.value
        if (state !is ViewState.QuickInventoryBottomSheetVisible) return
        try {
            val quantity = if (newQuantity.isNotEmpty()) {
                // ignore negative values, whitespace, and other invalid values
                if (newQuantity.toInt() in 0..Int.MAX_VALUE) {
                    newQuantity
                } else {
                    state.newQuantity
                }
            } else {
                newQuantity
            }
            _viewState.value = state.copy(newQuantity = quantity, isPendingUpdate = true)
        } catch (_: NumberFormatException) {}
    }

    private fun Product.isVariation(): Boolean {
        return this.parentId != 0L
    }

    fun onViewProductDetailsClicked() {
        tracker.track(AnalyticsEvent.PRODUCT_QUICK_INVENTORY_VIEW_PRODUCT_DETAILS_TAPPED)
        val productId: Long? = when (val state = viewState.value) {
            is ViewState.QuickInventoryBottomSheetVisible -> state.product.id
            else -> null
        }

        productId?.let { id ->
            triggerEvent(NavigateToProductDetailsEvent(id))
        }
    }

    fun onManageStockClicked() = launch {
        val state = viewState.value
        if (state !is ViewState.QuickInventoryBottomSheetVisible) return@launch

        val productInfo = state.product
        val updatedProductInfo = productInfo.copy(isStockManaged = true)
        val product = productRepository.getProduct(updatedProductInfo.id)
        if (product != null) {
            val result = if (product.isVariation()) {
                product.updateVariation(updatedProductInfo)
            } else {
                product.updateProduct(updatedProductInfo)
            }

            if (result.isSuccess) {
                _viewState.value = ViewState.QuickInventoryBottomSheetVisible(product = updatedProductInfo)
            } else {
                triggerEvent(
                    ShowUiStringSnackbar(
                        UiString.UiStringRes(
                            R.string.scan_to_update_inventory_failure_snackbar
                        )
                    )
                )
            }
        }
    }

    @Parcelize
    data class ProductInfo(
        val id: Long,
        val name: String,
        val imageUrl: String,
        val sku: String,
        val quantity: Int,
        val isStockManaged: Boolean,
    ) : Parcelable

    @Parcelize
    sealed class ViewState : Parcelable {
        data class QuickInventoryBottomSheetVisible(
            val product: ProductInfo,
            val isPendingUpdate: Boolean = false,
            val originalQuantity: String = product.quantity.toString(),
            val newQuantity: String = product.quantity.toString(),
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
