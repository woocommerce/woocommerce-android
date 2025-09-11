package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosProductEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogFetchProductsResult
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
                Failed("Local Catalog too large: $totalPages pages exceed maximum of $maxPages pages")

            class UnexpectedError(error: String) : Failed(error)
        }
    }

    suspend fun execute(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
        pageSize: Int,
        maxPages: Int
    ): WooPosSyncProductsResult {
        val fetchResult = runCatching {
            fetchAllPages(site, modifiedAfterGmt, pageSize, maxPages)
        }

        if (fetchResult.isFailure) {
            val error = fetchResult.exceptionOrNull()
            logger.e("Failed to fetch products: ${error?.message}")
            return when (error) {
                is CatalogTooLargeException -> error.toSyncResult()
                else -> WooPosSyncProductsResult.Failed.UnexpectedError(
                    error?.message ?: "Failed to fetch products"
                )
            }
        }

        val (products, serverDate) = fetchResult.getOrThrow()
        return posLocalCatalogStore.executeInTransaction {
            // TBD local catalog We need to either remove products that are no longer present on the server
            // or delete all products before we start inserting (low performance)
            // or soft-delete all products before we start inserting
            posLocalCatalogStore.upsertProducts(products)
        }.fold(
            onSuccess = {
                logger.d("Local Catalog transaction committed successfully")
                WooPosSyncProductsResult.Success(products.size, serverDate)
            },
            onFailure = { error ->
                handleTransactionError(error)
            }
        )
    }

    private suspend fun fetchAllPages(
        site: SiteModel,
        modifiedAfterGmt: String?,
        pageSize: Int,
        maxPages: Int
    ): Pair<List<WCPosProductEntity>, String> {
        var currentOffset = 0
        var pagesSynced = 0
        var totalSyncedProducts = 0
        var shouldContinue = true
        var firstPageServerDate: String? = null

        val products = mutableListOf<WCPosProductEntity>()

        while (shouldContinue) {
            val result = posLocalCatalogStore.fetchRecentlyModifiedProducts(
                site = site,
                pageSize = pageSize,
                modifiedAfterGmt = modifiedAfterGmt,
                offset = currentOffset,
            )

            result.fold(
                onSuccess = { syncResult ->
                    processPageResult(syncResult, pagesSynced, maxPages)
                    if (pagesSynced == 0) {
                        firstPageServerDate = syncResult.serverDate
                    }
                    products.addAll(syncResult.products)
                    totalSyncedProducts += syncResult.syncedCount
                    pagesSynced++

                    if (!syncResult.hasMore || syncResult.syncedCount == 0) {
                        logger.d("Local Catalog: No more products to sync")
                        shouldContinue = false
                    } else {
                        currentOffset = syncResult.nextOffset
                    }
                },
                onFailure = { error ->
                    logger.e("Local Catalog Sync failed on page ${pagesSynced + 1}: ${error.message}")
                    throw error
                }
            )
        }

        logger.d("Local catalog sync completed, $totalSyncedProducts products synced across $pagesSynced pages")
        return Pair(
            products.toList(),
            requireNotNull(firstPageServerDate, { "Can't be null since we throw an exception in the store layer." })
        )
    }

    private fun processPageResult(
        syncResult: WooPosLocalCatalogFetchProductsResult,
        pagesSynced: Int,
        maxPages: Int
    ) {
        if (pagesSynced == 0) {
            if (syncResult.totalPages > maxPages) {
                logger.e(
                    "Local Catalog too large: ${syncResult.totalPages} pages exceed maximum of $maxPages pages"
                )
                throw CatalogTooLargeException(syncResult.totalPages, maxPages)
            }
        }
        logger.d("Local Catalog page ${pagesSynced + 1} synced, ${syncResult.syncedCount} products")
    }

    private fun handleTransactionError(error: Throwable): WooPosSyncProductsResult {
        return when (error) {
            is CatalogTooLargeException -> {
                logger.e("Local Catalog too large, transaction rolled back")
                error.toSyncResult()
            }

            else -> {
                logger.e("Local Catalog Transaction failed and was rolled back: ${error.message}")
                WooPosSyncProductsResult.Failed.UnexpectedError(
                    error.message ?: "Local Catalog Transaction failed and was rolled back"
                )
            }
        }
    }

    internal class CatalogTooLargeException(
        val totalPages: Int,
        val maxPages: Int
    ) : Exception("Local Catalog too large: $totalPages pages exceed maximum of $maxPages pages") {
        fun toSyncResult(): WooPosSyncProductsResult.Failed.CatalogTooLarge {
            return WooPosSyncProductsResult.Failed.CatalogTooLarge(totalPages, maxPages)
        }
    }
}
