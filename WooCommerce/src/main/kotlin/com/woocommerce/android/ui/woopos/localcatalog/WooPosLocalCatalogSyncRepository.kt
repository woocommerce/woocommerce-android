package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncProductsAction.WooPosSyncProductsResult
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
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
class PosLocalCatalogSyncRepository @Inject constructor(
    private val posSyncProductsAction: WooPosSyncProductsAction,
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val dispatchers: CoroutineDispatchers,
    private val logger: WooPosLogWrapper,
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

    private suspend fun performSync(
        site: SiteModel,
        pageSize: Int,
        maxPages: Int,
        modifiedAfterGmt: String? = null,
    ): PosLocalCatalogSyncResult {
        val startTime = System.currentTimeMillis()

        logger.d("Starting sync for items modified after $modifiedAfterGmt, max pages: $maxPages")

        val productSyncResult = posSyncProductsAction.execute(site, modifiedAfterGmt, pageSize, maxPages)
        // TBD Local Catalog We'll want to trigger variations action here too

        val syncDuration = System.currentTimeMillis() - startTime

        return when (productSyncResult) {
            is WooPosSyncProductsResult.Success -> {
                // TBD Local Catalog we need to use store server timestamp
                val currentTime = System.currentTimeMillis()
                // TBD Local Catalog We need to store incremental and full sync timestamps separately
                syncTimestampManager.storeProductsLastSyncTimestamp(currentTime)

                PosLocalCatalogSyncResult.Success(
                    productsSynced = productSyncResult.productsSynced,
                    variationsSynced = 0,
                    syncDurationMs = syncDuration
                )
            }

            is WooPosSyncProductsResult.Failed.CatalogTooLarge -> {
                PosLocalCatalogSyncResult.Failure.CatalogTooLarge(
                    error = "Catalog too large: ${productSyncResult.totalPages} pages exceed maximum " +
                        "of ${productSyncResult.maxPages} pages",
                    totalPages = productSyncResult.totalPages,
                    maxPages = productSyncResult.maxPages
                )
            }

            is WooPosSyncProductsResult.Failed -> {
                PosLocalCatalogSyncResult.Failure.UnexpectedError(productSyncResult.error)
            }
        }
    }
}
