package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogFetchProductsResult
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject

private typealias ServerDate = String
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
        return runCatching {
            val isFullSync = modifiedAfterGmt == null

            val (products, serverDate) = fetchAllPages(site, modifiedAfterGmt, pageSize, maxPages)

            val trashProducts = if (!isFullSync) {
                fetchAllTrashProducts(site, pageSize)
            } else {
                emptyList()
            }

            val allProducts = products + trashProducts

            posLocalCatalogStore.executeInTransaction {
                if (isFullSync) {
                    posLocalCatalogStore.deleteAllProducts(
                        siteId = site.localId()
                    ).getOrThrow()
                }

                posLocalCatalogStore.upsertProducts(allProducts).getOrThrow()
            }.fold(
                onSuccess = {
                    logger.d("Local Catalog transaction committed successfully")
                    WooPosSyncProductsResult.Success(allProducts.size, serverDate)
                },
                onFailure = { error ->
                    handleTransactionError(error)
                }
            )
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error ->
                logger.e("Failed to sync products: ${error.message}")
                when (error) {
                    is CatalogTooLargeException -> error.toSyncResult()
                    else -> WooPosSyncProductsResult.Failed.UnexpectedError(
                        error.message ?: "Failed to sync products"
                    )
                }
            }
        )
    }

    private suspend fun fetchAllPages(
        site: SiteModel,
        modifiedAfterGmt: String?,
        pageSize: Int,
        maxPages: Int
    ): Pair<List<WooPosProductEntity>, ServerDate> {
        var currentOffset = 0
        var pagesSynced = 0
        var totalPages = maxPages
        var firstPageServerDate: String? = null

        val products = mutableListOf<WooPosProductEntity>()

        while (pagesSynced < totalPages) {
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
                    totalPages = syncResult.totalPages
                    pagesSynced++

                    if (!syncResult.hasMore || syncResult.syncedCount == 0) {
                        logger.d("Local Catalog: No more products to sync")
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

        logger.d("Local catalog sync completed, products synced across $pagesSynced pages")
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

    private suspend fun fetchAllTrashProducts(
        site: SiteModel,
        pageSize: Int
    ): List<WooPosProductEntity> {
        var currentOffset = 0
        var pagesSynced = 0
        val trashProducts = mutableListOf<WooPosProductEntity>()

        logger.d("Fetching all trash products for incremental sync")

        while (true) {
            val result = posLocalCatalogStore.fetchRecentlyModifiedProducts(
                site = site,
                pageSize = pageSize,
                modifiedAfterGmt = null,
                offset = currentOffset,
                includeStatus = listOf(CoreProductStatus.TRASH)
            )

            result.fold(
                onSuccess = { syncResult ->
                    trashProducts.addAll(syncResult.products)
                    pagesSynced++

                    if (!syncResult.hasMore || syncResult.syncedCount == 0) {
                        logger.d("Finished fetching trash products: ${trashProducts.size} total across $pagesSynced pages")
                        break
                    } else {
                        currentOffset = syncResult.nextOffset
                    }
                },
                onFailure = { error ->
                    logger.e("Failed to fetch trash products on page ${pagesSynced + 1}: ${error.message}")
                    throw error
                }
            )
        }

        return trashProducts.toList()
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
