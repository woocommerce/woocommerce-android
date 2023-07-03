package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import javax.inject.Inject

class CheckDigitRemoverFactory @Inject constructor() {
    fun getCheckDigitRemoverFor(barcodeFormat: BarcodeFormat): CheckDigitRemover {
        return when (barcodeFormat) {
            BarcodeFormat.FormatEAN13 -> EAN13CheckDigitRemover()
            BarcodeFormat.FormatEAN8 -> EAN8CheckDigitRemover()
            BarcodeFormat.FormatUPCA -> UPCCheckDigitRemover()
            BarcodeFormat.FormatUPCE -> UPCCheckDigitRemover()
            else -> throw IllegalStateException(
                "Cannot remove check digit for this barcode format: ${barcodeFormat.formatName}"
            )
        }
    }
}
