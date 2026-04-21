package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
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
    private val productDataSource: WooPosProductsDataSource,
    private val wooPosLogWrapper: WooPosLogWrapper,
) {
    suspend operator fun invoke(identifier: String): WooPosSearchByIdentifierResult {
        val syncStrategy = productDataSource.getCurrentSyncStrategy()

        val localResult = localSearcher(identifier, syncStrategy)
        // When product not found in local catalog, immediately return "not found" to avoid unnecessary remote call
        if (localResult.isSuccess || syncStrategy != WooPosProductsDataSource.SyncStrategy.REMOTE) {
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

    private fun meetsFilterRequirements(product: WooPosProductModel): Boolean {
        val hasValidStatus = product.status.value == filterConfig.filters[ProductFilterOption.STATUS]

        val meetsDownloadableRequirement = !product.isDownloadable ||
            filterConfig.filters[ProductFilterOption.DOWNLOADABLE] != DownloadableOptions.FALSE.toString()

        val hasValidType = filterConfig.includeTypes
            .filterNot { it == WCProductStore.IncludeType.Variable }
            .any { it.value.equals(product.type.value, ignoreCase = true) }

        return (hasValidStatus && meetsDownloadableRequirement && hasValidType)
            .also { meetsRequirements ->
                if (!meetsRequirements) {
                    wooPosLogWrapper.w(
                        "Product does not meet filter requirements: " +
                            "Status: $hasValidStatus, Downloadable: $meetsDownloadableRequirement," +
                            "Type: $hasValidType, Product: ${product.name}, Type: ${product.type}," +
                            "Status: ${product.status}"
                    )
                }
            }
    }

    private fun meetsVariationFilterRequirements(variation: WooPosVariation): Boolean {
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
                    wooPosLogWrapper.w(
                        "Variation does not meet filter requirements: " +
                            "Status: $hasValidStatus, Downloadable: $meetsDownloadableRequirement, " +
                            "Variation ID: ${variation.remoteVariationId}, Product ID: ${variation.remoteProductId}"
                    )
                }
            }
    }
}
