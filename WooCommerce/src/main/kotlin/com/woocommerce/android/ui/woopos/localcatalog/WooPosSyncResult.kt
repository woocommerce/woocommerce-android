package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel

sealed class PosLocalCatalogSyncResult {
    data class Success(
        val productsSynced: Int,
        val variationsSynced: Int,
        val syncDurationMs: Long
    ) : PosLocalCatalogSyncResult()

    sealed class Failure(val error: String) : PosLocalCatalogSyncResult() {
        class CatalogTooLarge(error: String, val totalPages: Int, val maxPages: Int) : Failure(error)
        class UnexpectedError(error: String) : Failure(error)
    }
}

sealed class WooPosSyncProductResult
sealed class WooPosSyncVariationResult
data class PosLocalCatalogProductSyncResult(val value: PosLocalCatalogSyncResult) : WooPosSyncProductResult()
data class PosLocalCatalogVariationSyncResult(val value: PosLocalCatalogSyncResult) : WooPosSyncVariationResult()

sealed class WooPosHybridProductSyncResult : WooPosSyncProductResult()

sealed class ProductsResult : WooPosHybridProductSyncResult() {
    data class Cached(val products: List<WooPosProductModel>) : ProductsResult()
    data class Remote(val productsResult: Result<List<WooPosProductModel>>) : ProductsResult()
}

sealed class WooPosHybridVariationSyncResult : WooPosSyncVariationResult()

sealed class VariationsResult : WooPosHybridVariationSyncResult() {
    data class Cached(val data: List<WooPosVariation>) : VariationsResult()
    data class Remote(val result: Result<List<WooPosVariation>>) : VariationsResult()
}
