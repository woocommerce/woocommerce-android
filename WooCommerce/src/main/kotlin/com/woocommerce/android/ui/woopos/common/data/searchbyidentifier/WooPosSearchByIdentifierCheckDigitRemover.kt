package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.barcode.removeCheckDigitIfPresent
import javax.inject.Inject

class WooPosSearchByIdentifierCheckDigitRemover @Inject constructor() {
    operator fun invoke(
        code: String,
        codeScannerResultFormat: WooPosBarcodeFormat
    ): String = codeScannerResultFormat.removeCheckDigitIfPresent(code)
}
