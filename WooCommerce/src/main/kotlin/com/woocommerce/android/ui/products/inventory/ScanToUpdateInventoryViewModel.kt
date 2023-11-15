package com.woocommerce.android.ui.products.inventory

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("UnusedPrivateMember", "EmptyFunctionBlock")
@HiltViewModel
class ScanToUpdateInventoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val fetchProductBySKU: FetchProductBySKU,
) : ScopedViewModel(savedState) {
    fun onBarcodeScanningResult(status: CodeScannerStatus) {
        when (status) {
            is CodeScannerStatus.Success -> {
                handleBarcodeScanningSuccess(status)
            }

            is CodeScannerStatus.Failure -> {
                handleBarcodeScanningFailure(status)
            }
        }
    }

    private fun handleBarcodeScanningFailure(status: CodeScannerStatus.Failure) {
        Log.e("ScanToUpdateInventory", "Barcode scanning failed ${status.error} ${status.type}")
    }

    private fun handleBarcodeScanningSuccess(status: CodeScannerStatus.Success) = launch {
        val productResult: Result<Product> =
            fetchProductBySKU(status.code, status.format)
        if (productResult.isSuccess) {
            Log.d("ScanToUpdateInventory", "Product found ${productResult.getOrNull()}")
            // TODO: show quick inventory update bottomsheet
        } else {
            Log.d("ScanToUpdateInventory", "Product not found ${productResult.getOrNull()}")
            // TODO: show product not found snack bar
        }
    }
}

