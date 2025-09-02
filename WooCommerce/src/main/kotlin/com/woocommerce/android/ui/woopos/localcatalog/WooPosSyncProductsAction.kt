package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject

class WooPosSyncProductsAction @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val logger: WooPosLogWrapper,
) {
    sealed class WooPosSyncProductsResult {
        data class Success(val productsSynced: Int, val serverDate: String?) : WooPosSyncProductsResult()

        sealed class Failed(val error: String) : WooPosSyncProductsResult() {
            class CatalogTooLarge(val totalPages: Int, val maxPages: Int) :
                Failed("Catalog too large: $totalPages pages exceed maximum of $maxPages pages")

            class UnexpectedError(error: String) : Failed(error)
        }
    }

    @Suppress("ReturnCount")
    suspend fun execute(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
        pageSize: Int,
        maxPages: Int
    ): WooPosSyncProductsResult {
        var currentOffset = 0
        var pagesSynced = 0
        var totalSyncedProducts = 0
        var shouldContinue = true
        var lastServerDate: String? = null

        while (shouldContinue) {
            /**
             * TBD Local Catalog We want to update the store to only fetch items and not store them so we can insert
             * all of them in a single transaction.
             */
            val result = posLocalCatalogStore.syncRecentlyModifiedProducts(
                site = site,
                pageSize = pageSize,
                modifiedAfterGmt = modifiedAfterGmt,
                offset = currentOffset
            )

            result.fold(
                onSuccess = { syncResult ->
                    // TBD Local Catalog We should first fetch the headers to decide if the catalog size is acceptable
                    if (pagesSynced == 0) {
                        if (syncResult.totalPages > maxPages) {
                            logger.e(
                                "Catalog too large: $syncResult.totalPages pages exceed maximum of $maxPages pages"
                            )
                            return WooPosSyncProductsResult.Failed.CatalogTooLarge(syncResult.totalPages, maxPages)
                        }
                    }

                    logger.d("Page ${pagesSynced + 1} synced, ${syncResult.syncedCount} products")
                    totalSyncedProducts += syncResult.syncedCount
                    pagesSynced++
                    lastServerDate = syncResult.serverDate

                    if (!syncResult.hasMore || syncResult.syncedCount == 0) {
                        logger.d("No more products to sync")
                        shouldContinue = false
                    } else {
                        currentOffset = syncResult.nextOffset
                    }
                },
                onFailure = { error ->
                    // TBD Local Catalog Add retry logic. We shouldn't fail when one page fails.
                    logger.e("Sync failed on page ${pagesSynced + 1}: ${error.message}")
                    return WooPosSyncProductsResult.Failed.UnexpectedError(error.message ?: "Unknown error")
                }
            )
        }

        logger.d("Products sync completed, $totalSyncedProducts products synced across $pagesSynced pages")
        return WooPosSyncProductsResult.Success(totalSyncedProducts, lastServerDate)
    }
}
