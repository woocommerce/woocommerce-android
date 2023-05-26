package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import com.google.mlkit.vision.barcode.common.Barcode as GoogleBarcode

class GoogleBarcodeFormatMapper @Inject constructor() {
    fun mapBarcodeFormat(format: Int): BarcodeFormat {
        return when (format) {
            GoogleBarcode.FORMAT_AZTEC -> BarcodeFormat.FormatAztec
            GoogleBarcode.FORMAT_CODABAR -> BarcodeFormat.FormatCodaBar
            GoogleBarcode.FORMAT_CODE_128 -> BarcodeFormat.FormatCode128
            GoogleBarcode.FORMAT_CODE_39 -> BarcodeFormat.FormatCode39
            GoogleBarcode.FORMAT_CODE_93 -> BarcodeFormat.FormatCode93
            GoogleBarcode.FORMAT_DATA_MATRIX -> BarcodeFormat.FormatDataMatrix
            GoogleBarcode.FORMAT_EAN_13 -> BarcodeFormat.FormatEAN13
            GoogleBarcode.FORMAT_EAN_8 -> BarcodeFormat.FormatEAN8
            GoogleBarcode.FORMAT_ITF -> BarcodeFormat.FormatITF
            GoogleBarcode.FORMAT_PDF417 -> BarcodeFormat.FormatPDF417
            GoogleBarcode.FORMAT_QR_CODE -> BarcodeFormat.FormatQRCode
            GoogleBarcode.FORMAT_UPC_A -> BarcodeFormat.FormatUPCA
            GoogleBarcode.FORMAT_UPC_E -> BarcodeFormat.FormatUPCE
            GoogleBarcode.FORMAT_UNKNOWN -> BarcodeFormat.FormatUnknown
            else -> BarcodeFormat.FormatUnknown
        }
    }

    sealed class BarcodeFormat(val formatName: String) : Parcelable {
        @Parcelize
        object FormatAztec : BarcodeFormat("aztec")
        @Parcelize
        object FormatCodaBar : BarcodeFormat("codabar")
        @Parcelize
        object FormatCode128 : BarcodeFormat("code 128")
        @Parcelize
        object FormatCode39 : BarcodeFormat("code 39")
        @Parcelize
        object FormatCode93 : BarcodeFormat("code 93")
        @Parcelize
        object FormatDataMatrix : BarcodeFormat("data matrix")
        @Parcelize
        object FormatEAN13 : BarcodeFormat("ean 13")
        @Parcelize
        object FormatEAN8 : BarcodeFormat("ean 8")
        @Parcelize
        object FormatITF : BarcodeFormat("itf")
        @Parcelize
        object FormatPDF417 : BarcodeFormat("pdf 417")
        @Parcelize
        object FormatQRCode : BarcodeFormat("qr code")
        @Parcelize
        object FormatUPCA : BarcodeFormat("upc a")
        @Parcelize
        object FormatUPCE : BarcodeFormat("upc e")
        @Parcelize
        object FormatUnknown : BarcodeFormat("unknown")
    }
}
