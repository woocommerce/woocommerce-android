package com.woocommerce.android.ui.products.inventory

import com.woocommerce.android.ui.barcodescanner.BarcodeScanningFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanToUpdateInventoryBarcodeScannerFragment : BarcodeScanningFragment() {
    override val isContinuousScanningEnabled = true
}
