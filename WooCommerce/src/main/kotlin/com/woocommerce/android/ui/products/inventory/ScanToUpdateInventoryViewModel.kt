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
    private val _viewState: MutableStateFlow<ViewState> = savedState.getStateFlow(this, ViewState.Scanning, "viewState")
    val viewState: StateFlow<ViewState> = _viewState

    fun onBarcodeScanningResult(status: CodeScannerStatus) {
        if (viewState.value !is ViewState.Scanning) return

        if (status is CodeScannerStatus.Success) {
            handleBarcodeScanningSuccess(status)
        }
    }

    fun onBottomSheetDismissed() {
        _viewState.value = ViewState.Scanning
    }

    private fun handleBarcodeScanningSuccess(status: CodeScannerStatus.Success) = launch {
        _viewState.value = ViewState.Loading
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringRes(R.string.scan_to_update_inventory_loading_product)))

        val productResult: Result<Product> = fetchProductBySKU(status.code, status.format)
        if (productResult.isSuccess) {
            val product = productResult.getOrNull()
            if (product != null) {
                val productInfo = ProductInfo(
                    name = product.name,
                    imageUrl = product.firstImageUrl.orEmpty(),
                    sku = product.sku,
                    quantity = product.stockQuantity.toInt(),
                )
                _viewState.value = ViewState.Result(productInfo)
            } else {
                handleProductNotFound(status.code)
            }
        } else {
            handleProductNotFound(status.code)
        }
    }

    private suspend fun handleProductNotFound(barcode: String) {
        triggerProductNotFoundSnackBar(barcode)
        delay(1000)
        _viewState.value = ViewState.Scanning
    }

    private fun triggerProductNotFoundSnackBar(barcode: String) {
        val message = resourceProvider.getString(R.string.scan_to_update_inventory_unable_to_find_product, barcode)
        triggerEvent(ShowUiStringSnackbar(UiString.UiStringText(message)))
    }

    @Parcelize
    data class ProductInfo(
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
    }
}
