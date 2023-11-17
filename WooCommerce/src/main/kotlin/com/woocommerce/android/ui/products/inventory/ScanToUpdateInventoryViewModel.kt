package com.woocommerce.android.ui.products.inventory

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanToUpdateInventoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val fetchProductBySKU: FetchProductBySKU,
) : ScopedViewModel(savedState) {
    private val barcodeScannerEnabled: MutableStateFlow<Boolean> = savedState.getStateFlow(this, true, "isBarcodeScannerEnabled")

    fun onBarcodeScanningResult(status: CodeScannerStatus) {
        if (!barcodeScannerEnabled.value) return

        if (status is CodeScannerStatus.Success) {
            handleBarcodeScanningSuccess(status)
        }
    }

    fun onBottomSheetDismissed() {
        barcodeScannerEnabled.value = true
    }

    private fun handleBarcodeScanningSuccess(status: CodeScannerStatus.Success) = launch {
        barcodeScannerEnabled.value = false
        val productResult: Result<Product> =
            fetchProductBySKU(status.code, status.format)
        if (productResult.isSuccess) {
            val product = productResult.getOrNull()
            if (product != null) {
                triggerEvent(OpenQuickInventoryUpdateBottomSheet(product))
            } else {
                Log.d("ScanToUpdateInventory", "Product is null ${productResult.getOrNull()}")
                // TODO: show product not found snack bar
            }
        } else {
            Log.d("ScanToUpdateInventory", "Product not found ${productResult.getOrNull()}")
            // TODO: show product not found snack bar
        }
    }

    data class OpenQuickInventoryUpdateBottomSheet(val product: Product) : MultiLiveEvent.Event()
}

