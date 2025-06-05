package com.woocommerce.android.ui.woopos.common.barcode

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class WooPosBarcodeFormat(val formatName: String) : Parcelable {
    @Parcelize
    object FormatAztec : WooPosBarcodeFormat("aztec")

    @Parcelize
    object FormatCodaBar : WooPosBarcodeFormat("codabar")

    @Parcelize
    object FormatCode128 : WooPosBarcodeFormat("code_128")

    @Parcelize
    object FormatCode39 : WooPosBarcodeFormat("code_39")

    @Parcelize
    object FormatCode93 : WooPosBarcodeFormat("code_93")

    @Parcelize
    object FormatDataMatrix : WooPosBarcodeFormat("data_matrix")

    @Parcelize
    object FormatEAN13 : WooPosBarcodeFormat("ean_13")

    @Parcelize
    object FormatEAN8 : WooPosBarcodeFormat("ean_8")

    @Parcelize
    object FormatITF : WooPosBarcodeFormat("itf")

    @Parcelize
    object FormatPDF417 : WooPosBarcodeFormat("pdf_417")

    @Parcelize
    object FormatQRCode : WooPosBarcodeFormat("qr_code")

    @Parcelize
    object FormatUPCA : WooPosBarcodeFormat("upc_a")

    @Parcelize
    object FormatUPCE : WooPosBarcodeFormat("upc_e")

    @Parcelize
    object FormatUnknown : WooPosBarcodeFormat("unknown")
}
