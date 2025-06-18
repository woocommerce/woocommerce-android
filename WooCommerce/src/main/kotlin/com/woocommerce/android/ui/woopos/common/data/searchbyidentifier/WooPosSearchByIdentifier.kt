package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationsTypesFilterConfig
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLogWrapper
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.DownloadableOptions
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.VariationFilterOption
import javax.inject.Inject

class WooPosSearchByIdentifier @Inject constructor(
    private val localSearcher: WooPosSearchByIdentifierLocal,
    private val remoteSearcher: WooPosSearchByIdentifierRemote,
    private val filterConfig: WooPosProductsTypesFilterConfig,
    private val variationFilterConfig: WooPosVariationsTypesFilterConfig,
    private val wooLogWrapper: WooLogWrapper,
) {
    suspend operator fun invoke(identifier: String): WooPosSearchByIdentifierResult {
        val localResult = localSearcher(identifier)
        if (localResult.isSuccess) {
            return filterUnsupportedProductResult(localResult)
        }

        val remoteResult = remoteSearcher(identifier)
        return filterUnsupportedProductResult(remoteResult)
    }

    private fun filterUnsupportedProductResult(result: WooPosSearchByIdentifierResult): WooPosSearchByIdentifierResult {
        return when (result) {
            is WooPosSearchByIdentifierResult.Success -> {
                if (meetsFilterRequirements(result.product)) {
                    result
                } else {
                    WooPosSearchByIdentifierResult
                        .Failure(WooPosSearchByIdentifierResult.Error.UnsupportedProduct((result.product.name)))
                }
            }

            is WooPosSearchByIdentifierResult.VariationSuccess -> {
                if (meetsVariationFilterRequirements(result.variation)) {
                    result
                } else {
                    WooPosSearchByIdentifierResult
                        .Failure(WooPosSearchByIdentifierResult.Error.UnsupportedProduct((result.parentProduct.name)))
                }
            }

            is WooPosSearchByIdentifierResult.Failure -> result
        }
    }

    private fun meetsFilterRequirements(product: Product): Boolean {
        val hasValidStatus = product.status?.value == filterConfig.filters[ProductFilterOption.STATUS]

        val meetsDownloadableRequirement = !product.isDownloadable ||
            filterConfig.filters[ProductFilterOption.DOWNLOADABLE] != DownloadableOptions.FALSE.toString()

        val hasValidType = filterConfig.includeTypes
            .filterNot { it == WCProductStore.IncludeType.Variable }
            .any { it.toString().equals(product.type, ignoreCase = true) }

        return (hasValidStatus && meetsDownloadableRequirement && hasValidType)
            .also { meetsRequirements ->
                if (!meetsRequirements) {
                    wooLogWrapper.w(
                        WooLog.T.POS,
                        "Product does not meet filter requirements: " +
                            "Status: $hasValidStatus, Downloadable: $meetsDownloadableRequirement," +
                            "Type: $hasValidType, Product: ${product.name}, Type: ${product.type}," +
                            "Status: ${product.status}"
                    )
                }
            }
    }

    private fun meetsVariationFilterRequirements(variation: ProductVariation): Boolean {
        val requiredStatus = variationFilterConfig.filters[VariationFilterOption.STATUS]
        val hasValidStatus = when (requiredStatus) {
            WooPosVariationsTypesFilterConfig.VARIATION_STATUS_PUBLISH -> variation.isVisible
            else -> false
        }

        val meetsDownloadableRequirement = !variation.isDownloadable ||
            variationFilterConfig.filters[VariationFilterOption.DOWNLOADABLE] != DownloadableOptions.FALSE.toString()

        return (hasValidStatus && meetsDownloadableRequirement)
            .also { meetsRequirements ->
                if (!meetsRequirements) {
                    wooLogWrapper.w(
                        WooLog.T.POS,
                        "Variation does not meet filter requirements: " +
                            "Status: $hasValidStatus, Downloadable: $meetsDownloadableRequirement, " +
                            "Variation ID: ${variation.remoteVariationId}, Product ID: ${variation.remoteProductId}"
                    )
                }
            }
    }
}
