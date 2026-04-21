package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel

sealed class PosLocalCatalogSyncResult {
    data class Success(
        val productsSynced: Int,
        val variationsSynced: Int,
        val syncDurationMs: Long,
        val generationDurationMs: Long? = null,
        val pollAttempts: Int? = null
    ) : PosLocalCatalogSyncResult()

    sealed class Failure(
        val error: String,
        val pollAttempts: Int? = null,
        val lastGenerationState: String? = null
    ) : PosLocalCatalogSyncResult() {
        class NetworkError(
            error: String,
            pollAttempts: Int? = null,
            lastGenerationState: String? = null
        ) : Failure(error, pollAttempts, lastGenerationState)

        class DatabaseError(
            error: String,
            pollAttempts: Int? = null,
            lastGenerationState: String? = null
        ) : Failure(error, pollAttempts, lastGenerationState)

        class InvalidResponse(
            error: String,
            pollAttempts: Int? = null,
            lastGenerationState: String? = null
        ) : Failure(error, pollAttempts, lastGenerationState)

        class UnexpectedError(
            error: String,
            pollAttempts: Int? = null,
            lastGenerationState: String? = null
        ) : Failure(error, pollAttempts, lastGenerationState)

        class CatalogGenerationTimeout(
            error: String,
            pollAttempts: Int? = null,
            lastGenerationState: String? = null
        ) : Failure(error, pollAttempts, lastGenerationState)

        fun withTrackingData(
            pollAttempts: Int,
            lastGenerationState: String?
        ): Failure = when (this) {
            is NetworkError -> NetworkError(error, pollAttempts, lastGenerationState)
            is DatabaseError -> DatabaseError(error, pollAttempts, lastGenerationState)
            is InvalidResponse -> InvalidResponse(error, pollAttempts, lastGenerationState)
            is CatalogGenerationTimeout -> CatalogGenerationTimeout(error, pollAttempts, lastGenerationState)
            is UnexpectedError -> UnexpectedError(error, pollAttempts, lastGenerationState)
        }
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
