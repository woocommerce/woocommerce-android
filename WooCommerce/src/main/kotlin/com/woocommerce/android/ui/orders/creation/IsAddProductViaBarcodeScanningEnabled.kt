package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsAddProductViaBarcodeScanningEnabled @Inject constructor() {
    operator fun invoke(): Boolean = FeatureFlag.IPP_ADD_PRODUCT_VIA_BARCODE_SCANNER.isEnabled()
}
