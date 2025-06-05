package com.woocommerce.android.ui.woopos.common.barcode

import javax.inject.Inject

class WooPosBarcodeCheckDigitRemoverFactory @Inject constructor() {
    fun getCheckDigitRemoverFor(barcodeFormat: WooPosBarcodeFormat): WooPosCheckDigitRemover {
        return when (barcodeFormat) {
            WooPosBarcodeFormat.FormatEAN13 -> WooPosEAN13CheckDigitRemover()
            WooPosBarcodeFormat.FormatEAN8 -> WooPosEAN8CheckDigitRemover()
            WooPosBarcodeFormat.FormatUPCA -> WooPosUPCCheckDigitRemover()
            WooPosBarcodeFormat.FormatUPCE -> WooPosUPCCheckDigitRemover()
            else -> throw IllegalStateException(
                "Cannot remove check digit for this barcode format: ${barcodeFormat.formatName}"
            )
        }
    }
}
