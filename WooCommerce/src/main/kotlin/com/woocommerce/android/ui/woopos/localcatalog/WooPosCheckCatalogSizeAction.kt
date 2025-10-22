package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject

class WooPosCheckCatalogSizeAction @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val logger: WooPosLogWrapper,
) {
    sealed class WooPosCheckCatalogSizeResult {
        object SizeAcceptable : WooPosCheckCatalogSizeResult()
        object SizeUnknown : WooPosCheckCatalogSizeResult()
        class CatalogTooLarge(val error: String) : WooPosCheckCatalogSizeResult()
    }

    @Suppress("ReturnCount")
    suspend fun execute(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
        maxTotalItems: Int,
    ): WooPosCheckCatalogSizeResult {
        logger.d("Checking catalog size before sync")

        val productsCountResult = posLocalCatalogStore.fetchProductsCount(site, modifiedAfterGmt)
        if (productsCountResult.isFailure) {
            logger.w(
                "Failed to fetch products count: ${productsCountResult.exceptionOrNull()?.message}. " +
                    "Skipping upfront catalog size check, will check during sync instead."
            )
            return WooPosCheckCatalogSizeResult.SizeUnknown
        }

        val variationsCountResult = posLocalCatalogStore.fetchVariationsCount(site, modifiedAfterGmt)
        if (variationsCountResult.isFailure) {
            logger.w(
                "Failed to fetch variations count: ${variationsCountResult.exceptionOrNull()?.message}. " +
                    "Skipping upfront catalog size check, will check during sync instead."
            )
            return WooPosCheckCatalogSizeResult.SizeUnknown
        }

        val totalProducts = productsCountResult.getOrThrow()
        val totalVariations = variationsCountResult.getOrThrow()
        val totalItems = totalProducts + totalVariations

        logger.d("Catalog size check: $totalProducts products + $totalVariations variations = $totalItems items")

        return if (totalItems > maxTotalItems) {
            logger.e("Catalog too large: $totalItems items exceed maximum of $maxTotalItems items")
            WooPosCheckCatalogSizeResult.CatalogTooLarge(
                error = "Catalog too large: $totalItems items " +
                    "(products: $totalProducts, variations: $totalVariations) " +
                    "exceed maximum of $maxTotalItems items"
            )
        } else {
            WooPosCheckCatalogSizeResult.SizeAcceptable
        }
    }
}
