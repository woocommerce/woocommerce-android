package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.orders.creation.CheckDigitRemoverFactory
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import javax.inject.Inject

class WooPosSearchByIdentifierLastDigitRemover @Inject constructor(
    private val checkDigitRemoverFactory: CheckDigitRemoverFactory
) {
    operator fun invoke(
        code: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): String? {
        if (codeScannerResultFormat.isEAN() || codeScannerResultFormat.isUPC()) {
            return checkDigitRemoverFactory.getCheckDigitRemoverFor(codeScannerResultFormat)
                .getSKUWithoutCheckDigit(code)
        }
        return null
    }

    private fun GoogleBarcodeFormatMapper.BarcodeFormat.isUPC() =
        this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCA ||
            this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCE

    private fun GoogleBarcodeFormatMapper.BarcodeFormat.isEAN() =
        this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN13 ||
            this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
}
