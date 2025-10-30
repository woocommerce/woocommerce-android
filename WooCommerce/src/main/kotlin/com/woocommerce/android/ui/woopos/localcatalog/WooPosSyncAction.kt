package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosPaginatedFetchResult
import javax.inject.Inject

class WooPosSyncAction @Inject constructor(
    private val productAction: WooPosSyncProductsAction,
    private val variationsAction: WooPosSyncVariationsAction
) {
    suspend fun syncProducts(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
        pageSize: Int,
        maxPages: Int
    ) = productAction.execute(
        site = site,
        modifiedAfterGmt = modifiedAfterGmt,
        pageSize = pageSize,
        maxPages = maxPages
    )

    suspend fun syncVariations(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
        pageSize: Int,
        maxPages: Int
    ) = variationsAction.execute(
        site = site,
        modifiedAfterGmt = modifiedAfterGmt,
        pageSize = pageSize,
        maxPages = maxPages
    )
}

private typealias ServerDate = String

sealed interface WooPosSyncResult {
    val syncedCount: Int
    val serverDate: String?

    data class Success(
        override val syncedCount: Int,
        override val serverDate: String?
    ) : WooPosSyncResult

    sealed class Failed(
        val error: String
    ) : WooPosSyncResult {
        override val syncedCount: Int = 0
        override val serverDate: String? = null

        data class CatalogTooLarge(
            val totalPages: Int,
            val maxPages: Int
        ) : Failed("Local Catalog too large: $totalPages pages exceed maximum of $maxPages pages")

        data class UnexpectedError(
            val errorMessage: String
        ) : Failed(errorMessage)
    }
}

class WooPosSyncProductsAction @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val logger: WooPosLogWrapper,
) {
    suspend fun execute(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
        pageSize: Int,
        maxPages: Int
    ): WooPosSyncResult {
        return runCatching {
            val isFullSync = modifiedAfterGmt == null

            val (products, trashProducts, serverDate) = coroutineScope {
                val regularProductsDeferred = async {
                    fetchAllPages(site, modifiedAfterGmt, pageSize, maxPages)
                }
                val trashProductsDeferred = async {
                    if (isFullSync) {
                        // We run incremental sync right after completing full sync -> no need to fetch trash products
                        emptyList()
                    } else {
                        fetchAllTrashProducts(site, pageSize)
                    }
                }

                val (products, serverDate) = regularProductsDeferred.await()
                val trashProducts = trashProductsDeferred.await()
                Triple(products, trashProducts, serverDate)
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
                    WooPosSyncResult.Success(allProducts.size, serverDate)
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
                    is CatalogTooLargeException -> WooPosSyncResult.Failed.CatalogTooLarge(
                        error.totalPages,
                        error.maxPages
                    )

                    else -> WooPosSyncResult.Failed.UnexpectedError(
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
        val helper = PaginatedSyncHelper(
            logger = logger,
            entityType = "products",
            fetchPage = { page ->
                posLocalCatalogStore.fetchRecentlyModifiedProducts(
                    site = site,
                    pageSize = pageSize,
                    modifiedAfterGmt = modifiedAfterGmt,
                    page = page,
                )
            }
        )

        return helper.fetchAllPages(maxPages)
    }

    private fun handleTransactionError(error: Throwable): WooPosSyncResult {
        return when (error) {
            is CatalogTooLargeException -> {
                logger.e("Local Catalog too large, transaction rolled back")
                WooPosSyncResult.Failed.CatalogTooLarge(error.totalPages, error.maxPages)
            }

            else -> {
                logger.e("Local Catalog Transaction failed and was rolled back: ${error.message}")
                WooPosSyncResult.Failed.UnexpectedError(
                    error.message ?: "Local Catalog Transaction failed and was rolled back"
                )
            }
        }
    }

    private suspend fun fetchAllTrashProducts(
        site: SiteModel,
        pageSize: Int
    ): List<WooPosProductEntity> {
        logger.d("Fetching all trash products for incremental sync")

        val helper = PaginatedSyncHelper(
            logger = logger,
            entityType = "trash products",
            fetchPage = { page ->
                posLocalCatalogStore.fetchRecentlyModifiedProducts(
                    site = site,
                    pageSize = pageSize,
                    modifiedAfterGmt = null,
                    page = page,
                    includeStatus = listOf(CoreProductStatus.TRASH)
                )
            }
        )

        val (trashProducts, _) = helper.fetchAllPages(Int.MAX_VALUE)
        return trashProducts
    }
}

class WooPosSyncVariationsAction @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val logger: WooPosLogWrapper,
) {
    suspend fun execute(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
        pageSize: Int,
        maxPages: Int
    ): WooPosSyncResult {
        return runCatching {
            val (variations, serverDate) = fetchAllPages(site, modifiedAfterGmt, pageSize, maxPages)

            posLocalCatalogStore.executeInTransaction {
                val isFullSync = modifiedAfterGmt == null
                if (isFullSync) {
                    posLocalCatalogStore.deleteAllVariations(
                        siteId = site.localId()
                    ).getOrThrow()
                }

                posLocalCatalogStore.upsertVariations(variations).getOrThrow()
            }.fold(
                onSuccess = {
                    logger.d("Local Catalog variations transaction committed successfully")
                    WooPosSyncResult.Success(variations.size, serverDate)
                },
                onFailure = { error ->
                    handleTransactionError(error)
                }
            )
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error ->
                logger.e("Failed to sync variations: ${error.message}")
                when (error) {
                    is CatalogTooLargeException -> WooPosSyncResult.Failed.CatalogTooLarge(
                        error.totalPages,
                        error.maxPages
                    )

                    else -> WooPosSyncResult.Failed.UnexpectedError(
                        error.message ?: "Failed to sync variations"
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
    ): Pair<List<WooPosVariationEntity>, String> {
        val helper = PaginatedSyncHelper(
            logger = logger,
            entityType = "variations",
            fetchPage = { page ->
                posLocalCatalogStore.fetchRecentlyModifiedVariations(
                    site = site,
                    modifiedAfterGmt = modifiedAfterGmt,
                    page = page,
                    pageSize = pageSize,
                )
            }
        )

        return helper.fetchAllPages(maxPages)
    }

    private fun handleTransactionError(error: Throwable): WooPosSyncResult {
        return when (error) {
            is CatalogTooLargeException -> {
                logger.e("Local Catalog variations too large, transaction rolled back")
                WooPosSyncResult.Failed.CatalogTooLarge(error.totalPages, error.maxPages)
            }

            else -> {
                logger.e("Local Catalog Variations Transaction failed and was rolled back: ${error.message}")
                WooPosSyncResult.Failed.UnexpectedError(
                    error.message ?: "Local Catalog Variations Transaction failed and was rolled back"
                )
            }
        }
    }
}

private class CatalogTooLargeException(
    val totalPages: Int,
    val maxPages: Int
) : Exception("Catalog too large: $totalPages pages exceed maximum of $maxPages pages")

private class PaginatedSyncHelper<T>(
    private val logger: WooPosLogWrapper,
    private val entityType: String,
    private val fetchPage: suspend (page: Int) -> Result<WooPosPaginatedFetchResult<T>>
) {
    suspend fun fetchAllPages(
        maxPages: Int
    ): Pair<List<T>, ServerDate> {
        var currentPage = 1
        var pagesSynced = 0
        var totalPages = maxPages
        var firstPageServerDate: String? = null

        val items = mutableListOf<T>()

        while (pagesSynced < totalPages) {
            val result = fetchPage(currentPage)

            result.fold(
                onSuccess = { syncResult ->
                    if (pagesSynced == 0) {
                        if (syncResult.totalPages > maxPages) {
                            logger.e(
                                "Local Catalog $entityType too large: ${syncResult.totalPages} " +
                                    "pages exceed maximum of $maxPages pages"
                            )
                            throw CatalogTooLargeException(syncResult.totalPages, maxPages)
                        }
                        firstPageServerDate = syncResult.serverDate
                        totalPages = syncResult.totalPages
                    }

                    items.addAll(syncResult.items)
                    pagesSynced++
                    logger.d(
                        "Local Catalog $entityType page $pagesSynced synced, " +
                            "${syncResult.syncedCount} $entityType"
                    )

                    if (!syncResult.hasMore || syncResult.syncedCount == 0) {
                        logger.d("Local Catalog: No more $entityType to sync")
                        break
                    } else {
                        currentPage = syncResult.nextPage
                    }
                },
                onFailure = { error ->
                    logger.e("Local Catalog $entityType sync failed on page ${pagesSynced + 1}: ${error.message}")
                    throw error
                }
            )
        }

        logger.d("Local catalog $entityType sync completed, $entityType synced across $pagesSynced pages")
        return Pair(
            items.toList(),
            requireNotNull(firstPageServerDate) { "Can't be null since we throw an exception in the store layer." }
        )
    }
}
