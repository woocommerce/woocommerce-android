package com.woocommerce.android.ui.products.inventory

import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningFragment
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanToUpdateInventoryBarcodeScannerFragment : BarcodeScanningFragment() {
    private val viewModel: ScanToUpdateInventoryViewModel by viewModels()

    override fun onScannedResult(status: CodeScannerStatus) {
        viewModel.onBarcodeScanningResult(status)
    }
}
