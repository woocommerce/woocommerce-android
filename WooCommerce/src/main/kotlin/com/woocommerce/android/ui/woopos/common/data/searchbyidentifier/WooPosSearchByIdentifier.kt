package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import org.wordpress.android.fluxc.store.WCProductStore.DownloadableOptions
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import javax.inject.Inject

class WooPosSearchByIdentifier @Inject constructor(
    private val localSearcher: WooPosSearchByIdentifierLocal,
    private val remoteSearcher: WooPosSearchByIdentifierRemote,
    private val filterConfig: WooPosProductsTypesFilterConfig,
) {
    suspend operator fun invoke(
        identifier: String,
        format: WooPosBarcodeFormat = WooPosBarcodeFormat.FormatUnknown
    ): WooPosSearchByIdentifierResult {
        val localProduct = localSearcher(identifier, format)
        if (localProduct != null) {
            return filterOutUnsupportedProducts(WooPosSearchByIdentifierResult.Success(localProduct))
        }

        val remoteResult = remoteSearcher(identifier, format)
        return filterOutUnsupportedProducts(remoteResult)
    }

    private fun filterOutUnsupportedProducts(result: WooPosSearchByIdentifierResult): WooPosSearchByIdentifierResult {
        if (result !is WooPosSearchByIdentifierResult.Success) {
            return result
        }

        return if (meetsFilterRequirements(result.product)) {
            result
        } else {
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.ProductNotFound)
        }
    }

    private fun meetsFilterRequirements(product: Product): Boolean {
        if (product.status?.value != filterConfig.filters[ProductFilterOption.STATUS]) {
            return false
        }

        if (product.isDownloadable
            && filterConfig.filters[ProductFilterOption.DOWNLOADABLE] == DownloadableOptions.FALSE.toString()
        ) {
            return false
        }

        return filterConfig.includeTypes.any {
            it.toString().equals(product.type, ignoreCase = true)
        }
    }

    fun onCleanup() {
        remoteSearcher.onCleanup()
    }
}
