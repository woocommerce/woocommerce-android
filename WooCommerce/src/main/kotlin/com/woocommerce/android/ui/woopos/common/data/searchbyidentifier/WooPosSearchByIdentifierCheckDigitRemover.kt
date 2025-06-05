package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.barcode.WooPosCheckBarcodeDigitRemoverFactory
import javax.inject.Inject

class WooPosSearchByIdentifierCheckDigitRemover @Inject constructor(
    private val checkDigitRemoverFactory: WooPosCheckBarcodeDigitRemoverFactory
) {
    operator fun invoke(
        code: String,
        codeScannerResultFormat: WooPosBarcodeFormat
    ): String? {
        if (codeScannerResultFormat.isEAN() || codeScannerResultFormat.isUPC()) {
            return checkDigitRemoverFactory.getCheckDigitRemoverFor(codeScannerResultFormat)
                .getCodeWithoutCheckDigit(code)
        }
        return null
    }

    private fun WooPosBarcodeFormat.isUPC() =
        this == WooPosBarcodeFormat.FormatUPCA ||
            this == WooPosBarcodeFormat.FormatUPCE

    private fun WooPosBarcodeFormat.isEAN() =
        this == WooPosBarcodeFormat.FormatEAN13 ||
            this == WooPosBarcodeFormat.FormatEAN8
}
