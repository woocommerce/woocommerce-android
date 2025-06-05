package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.data.WooPosBarcodeFormat
import javax.inject.Inject

class WooPosSearchByIdentifier @Inject constructor(
    private val localSearcher: WooPosSearchByIdentifierLocal,
    private val remoteSearcher: WooPosSearchByIdentifierRemote
) {
    suspend operator fun invoke(
        identifier: String,
        format: WooPosBarcodeFormat = WooPosBarcodeFormat.FormatUnknown
    ): WooPosSearchByIdentifierResult {
        val localProduct = localSearcher(identifier, format)
        if (localProduct != null) {
            return WooPosSearchByIdentifierResult.Success(localProduct)
        }

        return remoteSearcher(identifier, format)
    }

    fun onCleanup() {
        remoteSearcher.onCleanup()
    }
}
