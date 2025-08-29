package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.PosLocalCatalogStore
import javax.inject.Inject

class PosSyncProductsAction @Inject constructor(
    private val posLocalCatalogStore: PosLocalCatalogStore
) {
    companion object {
        const val PAGE_SIZE = 100
    }

    sealed class Result {
        data class Success(val productsSynced: Int) : Result()

        sealed class Failed(val error: String) : Result() {
            class CatalogTooLarge(val totalPages: Int, val maxPages: Int) :
                Failed("Catalog too large: $totalPages pages exceed maximum of $maxPages pages")
            class UnexpectedError(error: String) : Failed(error)
        }
    }

    @Suppress("ReturnCount")
    suspend fun execute(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
        maxPages: Int
    ): Result {
        var currentOffset = 0
        var pagesSynced = 0
        var totalSyncedProducts = 0
        var shouldContinue = true

        while (shouldContinue) {
            /**
             * TBD Local Catalog We want to update the store to only fetch items and not store them so we can insert
             * all of them in a single transaction.
             */
            val result = posLocalCatalogStore.syncRecentlyModifiedProducts(
                site = site,
                modifiedAfterGmt = modifiedAfterGmt,
                offset = currentOffset,
                pageSize = PAGE_SIZE
            )

            result.fold(
                onSuccess = { syncResult ->
                    // TBD Local Catalog We should first fetch the headers to decide if the catalog size is acceptable
                    if (pagesSynced == 0) {
                        if (syncResult.totalPages > maxPages) {
                            WooLog.e(
                                T.POS,
                                "Catalog too large: $syncResult.totalPages pages exceed maximum of $maxPages pages"
                            )
                            return Result.Failed.CatalogTooLarge(syncResult.totalPages, maxPages)
                        }
                    }

                    WooLog.d(T.POS, "Page ${pagesSynced + 1} synced, ${syncResult.syncedCount} products")
                    totalSyncedProducts += syncResult.syncedCount
                    pagesSynced++

                    if (!syncResult.hasMore || syncResult.syncedCount == 0) {
                        WooLog.d(T.POS, "No more products to sync")
                        shouldContinue = false
                    } else {
                        currentOffset = syncResult.nextOffset
                    }
                },
                onFailure = { error ->
                    // TBD Local Catalog Add retry logic. We shouldn't fail when one page fails.
                    WooLog.e(T.POS, "Sync failed on page ${pagesSynced + 1}: ${error.message}")
                    return Result.Failed.UnexpectedError(error.message ?: "Unknown error")
                }
            )
        }

        WooLog.d(T.POS, "Products sync completed, $totalSyncedProducts products synced across $pagesSynced pages")
        return Result.Success(totalSyncedProducts)
    }
}
