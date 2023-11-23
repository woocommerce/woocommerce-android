package com.woocommerce.android.ui.products.inventory

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ScanToUpdateInventoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val fetchProductBySKU: FetchProductBySKU,
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

        val productResult: Result<Product> =
            fetchProductBySKU(status.code, status.format)
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
                Log.d("ScanToUpdateInventory", "Product is null ${productResult.getOrNull()}")
                // TODO: show product not found snack bar
            }
        } else {
            Log.d("ScanToUpdateInventory", "Product not found ${productResult.getOrNull()}")
            // TODO: show product not found snack bar
        }
    }

    @Parcelize
    data class ProductInfo(
        val name: String,
        val imageUrl: String,
        val sku: String,
        val quantity: Int,
    ): Parcelable


    @Parcelize
    sealed class ViewState: Parcelable {
        object Scanning : ViewState()
        object Loading : ViewState()
        data class Result(val product: ProductInfo): ViewState()
    }
}
