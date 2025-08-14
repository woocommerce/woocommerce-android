package com.woocommerce.android.ui.woopos.settings.details.hardware.barcodescanner

import com.woocommerce.android.ui.woopos.common.util.ScannerInfo

data class WooPosSettingsHardwareBarcodeScannerState(
    val scannerInfo: ScannerInfo = ScannerInfo.NoScannerDetected
)
