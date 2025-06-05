package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.orders.creation.CheckDigitRemoverFactory
import com.woocommerce.android.ui.woopos.common.data.WooPosBarcodeFormat
import javax.inject.Inject

class WooPosSearchByIdentifierLastDigitRemover @Inject constructor(
    private val checkDigitRemoverFactory: CheckDigitRemoverFactory
) {
    operator fun invoke(
        code: String,
        codeScannerResultFormat: WooPosBarcodeFormat
    ): String? {
        if (codeScannerResultFormat.isEAN() || codeScannerResultFormat.isUPC()) {
            return checkDigitRemoverFactory.getCheckDigitRemoverFor(codeScannerResultFormat.toGoogleBarcodeFormat())
                .getSKUWithoutCheckDigit(code)
        }
        return null
    }

    private fun WooPosBarcodeFormat.isUPC() =
        this == WooPosBarcodeFormat.FormatUPCA ||
            this == WooPosBarcodeFormat.FormatUPCE

    private fun WooPosBarcodeFormat.isEAN() =
        this == WooPosBarcodeFormat.FormatEAN13 ||
            this == WooPosBarcodeFormat.FormatEAN8

    private fun WooPosBarcodeFormat.toGoogleBarcodeFormat(): com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat {
        return when (this) {
            WooPosBarcodeFormat.FormatAztec -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatAztec
            WooPosBarcodeFormat.FormatCodaBar -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatCodaBar
            WooPosBarcodeFormat.FormatCode128 -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode128
            WooPosBarcodeFormat.FormatCode39 -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode39
            WooPosBarcodeFormat.FormatCode93 -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode93
            WooPosBarcodeFormat.FormatDataMatrix -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatDataMatrix
            WooPosBarcodeFormat.FormatEAN13 -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN13
            WooPosBarcodeFormat.FormatEAN8 -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
            WooPosBarcodeFormat.FormatITF -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatITF
            WooPosBarcodeFormat.FormatPDF417 -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatPDF417
            WooPosBarcodeFormat.FormatQRCode -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatQRCode
            WooPosBarcodeFormat.FormatUPCA -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCA
            WooPosBarcodeFormat.FormatUPCE -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCE
            WooPosBarcodeFormat.FormatUnknown -> com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat.FormatUnknown
        }
    }
}
