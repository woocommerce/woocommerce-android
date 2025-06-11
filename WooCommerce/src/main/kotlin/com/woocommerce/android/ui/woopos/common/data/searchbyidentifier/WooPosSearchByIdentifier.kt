package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import org.wordpress.android.fluxc.store.WCProductStore
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
            return filterUnsupportedProductResult(WooPosSearchByIdentifierResult.Success(localProduct))
        }

        val remoteResult = remoteSearcher(identifier, format)
        return filterUnsupportedProductResult(remoteResult)
    }

    private fun filterUnsupportedProductResult(result: WooPosSearchByIdentifierResult): WooPosSearchByIdentifierResult {
        if (result !is WooPosSearchByIdentifierResult.Success) {
            return result
        }

        return if (meetsFilterRequirements(result.product)) {
            result
        } else {
            WooPosSearchByIdentifierResult
                .Failure(WooPosSearchByIdentifierResult.Error.UnsupportedProduct(result.product.name))
        }
    }

    private fun meetsFilterRequirements(product: Product): Boolean {
        val hasValidStatus = product.status?.value == filterConfig.filters[ProductFilterOption.STATUS]

        val meetsDownloadableRequirement = !product.isDownloadable ||
            filterConfig.filters[ProductFilterOption.DOWNLOADABLE] != DownloadableOptions.FALSE.toString()

        val hasValidType = filterConfig.includeTypes
            .filterNot { it == WCProductStore.IncludeType.Variable }
            .any { it.toString().equals(product.type, ignoreCase = true) }

        return hasValidStatus && meetsDownloadableRequirement && hasValidType
    }

    fun onCleanup() {
        remoteSearcher.onCleanup()
    }
}
