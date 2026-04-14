package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogError
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosPaginatedFetchResult
import javax.inject.Inject

class WooPosSyncAction @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val logger: WooPosLogWrapper,
    private val syncWithFts: WooPosLocalCatalogSyncWithFts,
    private val analyticsTracker: WooPosAnalyticsTracker,
) {
    suspend fun syncCatalog(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
        pageSize: Int,
        maxPages: Int
    ): WooPosSyncResult {
        return runCatching {
            val isFullSync = modifiedAfterGmt == null
            val fetchResults = fetchAllCatalogData(site, modifiedAfterGmt, pageSize, maxPages, isFullSync)

            updateDatabaseWithFetchedItems(site, fetchResults, isFullSync)
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error ->
                if (error is CancellationException) throw error
                handleSyncError(error)
            }
        )
    }

    private suspend fun fetchAllCatalogData(
        site: SiteModel,
        modifiedAfterGmt: String?,
        pageSize: Int,
        maxPages: Int,
        isFullSync: Boolean
    ): FetchResults = coroutineScope {
        val posProductsDeferred = async {
            fetchAllProductPages(
                site,
                modifiedAfterGmt,
                pageSize,
                maxPages,
                posProductsOnly = true
            )
        }
        val trashProductsDeferred = async {
            if (isFullSync) {
                // We run incremental sync right after completing full sync -> no need to fetch trash products
                emptyList()
            } else {
                fetchAllTrashProducts(site, modifiedAfterGmt, pageSize)
            }
        }

        val allProductsDeferred = async {
            if (isFullSync) {
                emptyList()
            } else {
                fetchAllProductPages(
                    site,
                    modifiedAfterGmt,
                    pageSize,
                    maxPages,
                    posProductsOnly = false
                ).first
            }
        }
        val variationsDeferred = async {
            fetchAllVariationPages(site, modifiedAfterGmt, pageSize, maxPages)
        }

        val (posProducts, productsServerDate) = posProductsDeferred.await()
        val trashProducts = trashProductsDeferred.await()
        val allProducts = allProductsDeferred.await()
        val (variations, variationsServerDate) = variationsDeferred.await()

        /**
         * Products hidden from POS don't appear in the pos_products_only=true response—they're simply omitted.
         * To detect these, we compare against a second request without the flag: items present there but
         * missing from the pos_products_only=true response have been hidden and should be removed from the local DB.
         *
         * This logic is skipped for full sync, since full sync always purges the whole database.
         */
        val productsRecentlyMarkedAsHiddenInPos =
            if (isFullSync) {
                emptyList()
            } else {
                findProductsMarkedAsHiddenInPos(
                    posProducts = posProducts,
                    allProducts = allProducts
                )
            }

        FetchResults(
            products = posProducts,
            trashProducts = trashProducts,
            productsToRemove = productsRecentlyMarkedAsHiddenInPos,
            productsServerDate = productsServerDate,
            variations = variations,
            variationsServerDate = variationsServerDate
        )
    }

    private suspend fun updateDatabaseWithFetchedItems(
        site: SiteModel,
        fetchResults: FetchResults,
        isFullSync: Boolean
    ): WooPosSyncResult {
        val allProducts = fetchResults.products + fetchResults.trashProducts
        val siteIdString = site.localId().value.toString()

        return posLocalCatalogStore.executeInTransaction {
            if (isFullSync) {
                posLocalCatalogStore.deleteAllProducts(site.localId()).getOrThrow()
                posLocalCatalogStore.deleteAllVariations(site.localId()).getOrThrow()
            }

            if (fetchResults.productsToRemove.isNotEmpty()) {
                posLocalCatalogStore.deleteProducts(
                    site.localId(),
                    fetchResults.productsToRemove
                ).getOrThrow()
            }

            posLocalCatalogStore.upsertProducts(allProducts).getOrThrow()
            posLocalCatalogStore.upsertVariations(fetchResults.variations).getOrThrow()

            val ftsSyncResult = if (isFullSync) {
                syncWithFts.syncFtsForFullSync(siteIdString, allProducts, fetchResults.variations)
            } else {
                val trashProductIds = fetchResults.trashProducts.map { it.remoteId }
                syncWithFts.syncFtsForIncrementalSync(
                    siteIdString,
                    fetchResults.products,
                    fetchResults.variations,
                    fetchResults.productsToRemove + trashProductIds
                )
            }
            ftsSyncResult?.let {
                analyticsTracker.track(
                    WooPosAnalyticsEvent.Event.FtsIndexBuilt(
                        syncType = if (isFullSync) "full" else "incremental",
                        indexDurationMs = it.durationMs,
                        productsIndexed = it.productsIndexed,
                    )
                )
            }
        }.fold(
            onSuccess = {
                logger.d("Local Catalog transaction committed successfully")
                WooPosSyncResult.Success(
                    productsSynced = allProducts.size,
                    variationsSynced = fetchResults.variations.size,
                    productsServerDate = fetchResults.productsServerDate,
                    variationsServerDate = fetchResults.variationsServerDate
                )
            },
            onFailure = { error -> handleTransactionError(error) }
        )
    }

    private fun handleSyncError(error: Throwable): WooPosSyncResult {
        logger.e("Failed to sync catalog: ${error.message}")
        return when (error) {
            is CatalogTooLargeException -> WooPosSyncResult.Failed.CatalogTooLarge(
                error.totalPages,
                error.maxPages
            )

            is WooPosLocalCatalogError.NetworkError -> WooPosSyncResult.Failed.NetworkError(
                errorMessage = error.errorMessage.ifBlank {
                    "Network error${error.code?.let { " (code: $it)" } ?: ""}"
                }
            )

            is WooPosLocalCatalogError.DatabaseError -> WooPosSyncResult.Failed.DatabaseError(
                errorMessage = error.errorMessage.ifBlank {
                    "Database error (${error.throwable?.let { it::class.simpleName } ?: "unknown"})"
                },
                throwable = error.throwable
            )

            is WooPosLocalCatalogError.InvalidResponse -> WooPosSyncResult.Failed.InvalidResponse(
                errorMessage = error.errorMessage.ifBlank { "Invalid response" }
            )

            is WooPosLocalCatalogError.EmptyResponse -> WooPosSyncResult.Failed.InvalidResponse(
                errorMessage = error.message
            )

            else -> WooPosSyncResult.Failed.UnexpectedError(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "Failed to sync catalog (${error::class.simpleName})"
            )
        }
    }

    private data class FetchResults(
        val products: List<WooPosProductEntity>,
        val trashProducts: List<WooPosProductEntity>,
        val productsToRemove: List<RemoteId>,
        val productsServerDate: String,
        val variations: List<WooPosVariationEntity>,
        val variationsServerDate: String
    )

    private suspend fun fetchAllProductPages(
        site: SiteModel,
        modifiedAfterGmt: String?,
        pageSize: Int,
        maxPages: Int,
        posProductsOnly: Boolean
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
                    posProductsOnly = posProductsOnly,
                )
            }
        )

        return helper.fetchAllPages(maxPages)
    }

    private suspend fun fetchAllVariationPages(
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
                    posProductsOnly = true,
                    modifiedAfterGmt = modifiedAfterGmt,
                    page = page,
                    pageSize = pageSize,
                )
            }
        )

        return helper.fetchAllPages(maxPages)
    }

    private suspend fun fetchAllTrashProducts(
        site: SiteModel,
        modifiedAfterGmt: String?,
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
                    posProductsOnly = true,
                    modifiedAfterGmt = modifiedAfterGmt,
                    page = page,
                    includeStatus = listOf(CoreProductStatus.TRASH),
                )
            }
        )

        val (trashProducts, _) = helper.fetchAllPages(Int.MAX_VALUE)
        return trashProducts
    }

    private fun findProductsMarkedAsHiddenInPos(
        posProducts: List<WooPosProductEntity>,
        allProducts: List<WooPosProductEntity>
    ): List<RemoteId> {
        val posProductIds = posProducts.map { it.remoteId }.toSet()
        return allProducts
            .filter { it.remoteId !in posProductIds }
            .map { it.remoteId }
    }

    private fun handleTransactionError(error: Throwable): WooPosSyncResult {
        return when (error) {
            is CatalogTooLargeException -> {
                logger.e("Local Catalog too large, transaction rolled back")
                WooPosSyncResult.Failed.CatalogTooLarge(error.totalPages, error.maxPages)
            }

            is WooPosLocalCatalogError.DatabaseError -> {
                logger.e("Local Catalog Transaction failed and was rolled back: ${error.message}")
                WooPosSyncResult.Failed.DatabaseError(
                    errorMessage = error.errorMessage.ifBlank {
                        "Transaction database error (${error.throwable?.let { it::class.simpleName } ?: "unknown"})"
                    },
                    throwable = error.throwable
                )
            }

            else -> {
                logger.e("Local Catalog Transaction failed and was rolled back: ${error.message}")
                WooPosSyncResult.Failed.UnexpectedError(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "Transaction failed and was rolled back (${error::class.simpleName})"
                )
            }
        }
    }
}

private typealias ServerDate = String

sealed interface WooPosSyncResult {
    val productsSynced: Int
    val variationsSynced: Int
    val productsServerDate: String?
    val variationsServerDate: String?

    data class Success(
        override val productsSynced: Int,
        override val variationsSynced: Int,
        override val productsServerDate: String?,
        override val variationsServerDate: String?
    ) : WooPosSyncResult

    sealed class Failed(
        val error: String
    ) : WooPosSyncResult {
        override val productsSynced: Int = 0
        override val variationsSynced: Int = 0
        override val productsServerDate: String? = null
        override val variationsServerDate: String? = null

        data class CatalogTooLarge(
            val totalPages: Int,
            val maxPages: Int
        ) : Failed("Local Catalog too large: $totalPages pages exceed maximum of $maxPages pages")

        data class NetworkError(
            val errorMessage: String
        ) : Failed(errorMessage)

        data class DatabaseError(
            val errorMessage: String,
            val throwable: Throwable? = null
        ) : Failed(errorMessage)

        data class InvalidResponse(
            val errorMessage: String
        ) : Failed(errorMessage)

        data class UnexpectedError(
            val errorMessage: String
        ) : Failed(errorMessage)
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
