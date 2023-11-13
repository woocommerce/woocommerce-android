package com.woocommerce.android.ui.products.inventory

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Suppress("UnusedPrivateMember", "EmptyFunctionBlock")
@HiltViewModel
class ScanToUpdateInventoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productRepository: ProductDetailRepository,

) : ScopedViewModel(savedState) {
    fun onBarcodeScanningResult(status: CodeScannerStatus) {
    }
}
