package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncProductsAction.WooPosSyncProductsResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncVariationsAction.WooPosSyncVariationsResult
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class WooPosLocalCatalogSyncRepository @Inject constructor(
    private val posSyncProductsAction: WooPosSyncProductsAction,
    private val posSyncVariationsAction: WooPosSyncVariationsAction,
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val dispatchers: CoroutineDispatchers,
    private val logger: WooPosLogWrapper,
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
) {
    companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES_PER_FULL_SYNC = 10
        const val MAX_PAGES_PER_INCREMENTAL_SYNC = 3
    }

    suspend fun syncLocalCatalogFull(site: SiteModel): PosLocalCatalogSyncResult = withContext(dispatchers.io) {
        return@withContext performSync(site = site, pageSize = PAGE_SIZE, maxPages = MAX_PAGES_PER_FULL_SYNC)
    }

    suspend fun syncLocalCatalogIncremental(site: SiteModel): PosLocalCatalogSyncResult = withContext(dispatchers.io) {
        val lastSyncTimestamp = syncTimestampManager.getProductsLastSyncTimestamp() ?: 0L
        val modifiedAfterGmt = syncTimestampManager.formatTimestampForApi(lastSyncTimestamp)

        return@withContext performSync(
            site = site,
            modifiedAfterGmt = modifiedAfterGmt,
            pageSize = PAGE_SIZE,
            maxPages = MAX_PAGES_PER_INCREMENTAL_SYNC,
        )
    }

    @Suppress("ReturnCount")
    private suspend fun performSync(
        site: SiteModel,
        pageSize: Int,
        maxPages: Int,
        modifiedAfterGmt: String? = null,
    ): PosLocalCatalogSyncResult {
        val startTime = System.currentTimeMillis()

        logger.d("Starting sync for items modified after $modifiedAfterGmt, max pages: $maxPages")

        val productSyncResult = syncProducts(site, modifiedAfterGmt, pageSize, maxPages)
        if (productSyncResult is WooPosSyncProductsResult.Failed) {
            return productSyncResult.toPosLocalCatalogSyncFailure()
        }

        val variationSyncResult = syncVariations(site, modifiedAfterGmt, pageSize, maxPages)
        if (variationSyncResult is WooPosSyncVariationsResult.Failed) {
            return variationSyncResult.toPosLocalCatalogSyncFailure()
        }

        val syncDuration = System.currentTimeMillis() - startTime

        return PosLocalCatalogSyncResult.Success(
            productsSynced = (productSyncResult as WooPosSyncProductsResult.Success).productsSynced,
            variationsSynced = (variationSyncResult as WooPosSyncVariationsResult.Success).variationsSynced,
            syncDurationMs = syncDuration
        )
    }

    private suspend fun syncProducts(
        site: SiteModel,
        modifiedAfterGmt: String?,
        pageSize: Int,
        maxPages: Int
    ): WooPosSyncProductsResult {
        val result = posSyncProductsAction.execute(site, modifiedAfterGmt, pageSize, maxPages)

        if (result is WooPosSyncProductsResult.Success) {
            result.serverDate?.let { serverDate ->
                syncTimestampManager.parseTimestampFromApi(serverDate)?.let { timestamp ->
                    syncTimestampManager.storeProductsLastSyncTimestamp(timestamp)
                    logger.d("Stored products sync timestamp: $serverDate")
                }
            }
        }

        return result
    }

    private suspend fun syncVariations(
        site: SiteModel,
        modifiedAfterGmt: String?,
        pageSize: Int,
        maxPages: Int
    ): WooPosSyncVariationsResult {
        val result = posSyncVariationsAction.execute(site, modifiedAfterGmt, pageSize, maxPages)

        if (result is WooPosSyncVariationsResult.Success) {
            result.serverDate?.let { serverDate ->
                syncTimestampManager.parseTimestampFromApi(serverDate)?.let { timestamp ->
                    syncTimestampManager.storeVariationsLastSyncTimestamp(timestamp)
                    logger.d("Stored variations sync timestamp: $serverDate")
                }
            }
        }

        return result
    }

    suspend fun getProductCount(site: SiteModel): Int =
        posLocalCatalogStore.getProductCount(LocalId(site.id)).getOrElse { 0 }

    suspend fun getVariationCount(site: SiteModel): Int =
        posLocalCatalogStore.getVariationCount(LocalId(site.id)).getOrElse { 0 }
}

private fun WooPosSyncProductsResult.Failed.toPosLocalCatalogSyncFailure(): PosLocalCatalogSyncResult.Failure {
    return when (this) {
        is WooPosSyncProductsResult.Failed.CatalogTooLarge -> {
            PosLocalCatalogSyncResult.Failure.CatalogTooLarge(
                error = "Product catalog too large: $totalPages pages exceed maximum of $maxPages pages",
                totalPages = totalPages,
                maxPages = maxPages
            )
        }
        else -> {
            PosLocalCatalogSyncResult.Failure.UnexpectedError(error)
        }
    }
}

private fun WooPosSyncVariationsResult.Failed.toPosLocalCatalogSyncFailure(): PosLocalCatalogSyncResult.Failure {
    return when (this) {
        is WooPosSyncVariationsResult.Failed.CatalogTooLarge -> {
            PosLocalCatalogSyncResult.Failure.CatalogTooLarge(
                error = "Variations catalog too large: $totalPages pages exceed maximum of $maxPages pages",
                totalPages = totalPages,
                maxPages = maxPages
            )
        }
        else -> {
            PosLocalCatalogSyncResult.Failure.UnexpectedError(error)
        }
    }
}
