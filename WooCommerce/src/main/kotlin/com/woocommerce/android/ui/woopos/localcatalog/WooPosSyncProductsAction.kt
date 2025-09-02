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
        data class Success(val productsSynced: Int) : WooPosSyncProductsResult()

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
        return posLocalCatalogStore.executeInTransaction {
            var currentOffset = 0
            var pagesSynced = 0
            var totalSyncedProducts = 0
            var shouldContinue = true

            while (shouldContinue) {
                val result = posLocalCatalogStore.syncRecentlyModifiedProducts(
                    site = site,
                    pageSize = pageSize,
                    modifiedAfterGmt = modifiedAfterGmt,
                    offset = currentOffset,
                    storeInDb = true
                )

                result.fold(
                    onSuccess = { syncResult ->
                        if (pagesSynced == 0) {
                            if (syncResult.totalPages > maxPages) {
                                logger.e(
                                    "Catalog too large: ${syncResult.totalPages} pages exceed maximum of $maxPages pages"
                                )
                                throw CatalogTooLargeException(syncResult.totalPages, maxPages)
                            }
                        }

                        logger.d("Page ${pagesSynced + 1} synced, ${syncResult.syncedCount} products")
                        totalSyncedProducts += syncResult.syncedCount
                        pagesSynced++

                        if (!syncResult.hasMore || syncResult.syncedCount == 0) {
                            logger.d("No more products to sync")
                            shouldContinue = false
                        } else {
                            currentOffset = syncResult.nextOffset
                        }
                    },
                    onFailure = { error ->
                        logger.e("Sync failed on page ${pagesSynced + 1}: ${error.message}")
                        throw error
                    }
                )
            }

            logger.d("Products sync completed, $totalSyncedProducts products synced across $pagesSynced pages")
            WooPosSyncProductsResult.Success(totalSyncedProducts)
        }.fold(
            onSuccess = { result ->
                logger.d("Transaction committed successfully")
                result
            },
            onFailure = { error ->
                when (error) {
                    is CatalogTooLargeException -> {
                        logger.e("Catalog too large, transaction rolled back")
                        error.toSyncResult()
                    }
                    else -> {
                        logger.e("Transaction failed and was rolled back: ${error.message}")
                        WooPosSyncProductsResult.Failed.UnexpectedError(
                            error.message ?: "Transaction failed and was rolled back"
                        )
                    }
                }
            }
        )
    }

    internal class CatalogTooLargeException(
        val totalPages: Int,
        val maxPages: Int
    ) : Exception("Catalog too large: $totalPages pages exceed maximum of $maxPages pages") {
        fun toSyncResult(): WooPosSyncProductsResult.Failed.CatalogTooLarge {
            return WooPosSyncProductsResult.Failed.CatalogTooLarge(totalPages, maxPages)
        }
    }
}
