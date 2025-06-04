package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import javax.inject.Inject

class WooPosSearchByIdentifier @Inject constructor(
    private val localSearcher: WooPosSearchByIdentifierLocal,
    private val remoteSearcher: WooPosSearchByIdentifierRemote
) {
    fun onCleanup() {
        remoteSearcher.onCleanup()
    }

    suspend operator fun invoke(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): WooPosSearchByIdentifierResult {
        val localProduct = localSearcher.searchProduct(codeScannerResultCode, codeScannerResultFormat)
        if (localProduct != null) {
            return WooPosSearchByIdentifierResult.Success(localProduct)
        }

        return remoteSearcher.searchProduct(codeScannerResultCode, codeScannerResultFormat)
    }
}
