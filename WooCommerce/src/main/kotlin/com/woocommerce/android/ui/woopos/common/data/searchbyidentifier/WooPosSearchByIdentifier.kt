package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationsTypesFilterConfig
import org.wordpress.android.fluxc.store.WCProductStore.DownloadableOptions
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.VariationFilterOption
import javax.inject.Inject

class WooPosSearchByIdentifier @Inject constructor(
    private val localSearcher: WooPosSearchByIdentifierLocal,
    private val remoteSearcher: WooPosSearchByIdentifierRemote,
    private val filterConfig: WooPosProductsTypesFilterConfig,
    private val variationFilterConfig: WooPosVariationsTypesFilterConfig,
) {
    suspend operator fun invoke(
        identifier: String,
        format: WooPosBarcodeFormat = WooPosBarcodeFormat.FormatUnknown
    ): WooPosSearchByIdentifierResult {
        val localResult = localSearcher(identifier, format)
        if (localResult != null) {
            return filterUnsupportedProductResult(localResult)
        }

        val remoteResult = remoteSearcher(identifier, format)
        return filterUnsupportedProductResult(remoteResult)
    }

    private fun filterUnsupportedProductResult(result: WooPosSearchByIdentifierResult): WooPosSearchByIdentifierResult {
        return when (result) {
            is WooPosSearchByIdentifierResult.Success -> {
                if (meetsFilterRequirements(result.product)) {
                    result
                } else {
                    WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.UnsupportedProduct)
                }
            }
            is WooPosSearchByIdentifierResult.VariationSuccess -> {
                if (meetsVariationFilterRequirements(result.variation)) {
                    result
                } else {
                    WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.UnsupportedProduct)
                }
            }
            is WooPosSearchByIdentifierResult.Failure -> result
        }
    }

    private fun meetsFilterRequirements(product: Product): Boolean {
        val hasValidStatus = product.status?.value == filterConfig.filters[ProductFilterOption.STATUS]

        val meetsDownloadableRequirement = !product.isDownloadable ||
            filterConfig.filters[ProductFilterOption.DOWNLOADABLE] != DownloadableOptions.FALSE.toString()

        val hasValidType = filterConfig.includeTypes.any {
            it.toString().equals(product.type, ignoreCase = true)
        }

        return hasValidStatus && meetsDownloadableRequirement && hasValidType
    }

    private fun meetsVariationFilterRequirements(variation: ProductVariation): Boolean {
        val requiredStatus = variationFilterConfig.filters[VariationFilterOption.STATUS]
        val hasValidStatus = when (requiredStatus) {
            "publish" -> variation.isVisible
            else -> false
        }

        val meetsDownloadableRequirement = !(
            variation.isDownloadable &&
                variationFilterConfig.filters[VariationFilterOption.DOWNLOADABLE] ==
                DownloadableOptions.FALSE.toString()
            )

        return hasValidStatus && meetsDownloadableRequirement
    }

    fun onCleanup() {
        remoteSearcher.onCleanup()
    }
}
