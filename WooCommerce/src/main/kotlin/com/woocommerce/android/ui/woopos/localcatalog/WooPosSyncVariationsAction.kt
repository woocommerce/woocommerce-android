package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosVariationsFetchResult
import javax.inject.Inject

class WooPosSyncVariationsAction @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val logger: WooPosLogWrapper,
) {
    sealed class WooPosSyncVariationsResult {
        data class Success(val variationsSynced: Int, val serverDate: String?) : WooPosSyncVariationsResult()

        sealed class Failed(val error: String) : WooPosSyncVariationsResult() {
            class CatalogTooLarge(val totalPages: Int, val maxPages: Int) :
                Failed("Catalog too large: $totalPages pages exceed maximum of $maxPages pages")

            class UnexpectedError(error: String) : Failed(error)
        }
    }

    suspend fun execute(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
        pageSize: Int,
        maxPages: Int
    ): WooPosSyncVariationsResult {
        return runCatching {
            val isFullSync = modifiedAfterGmt == null

            val (variations, trashVariations, serverDate) = coroutineScope {
                val regularVariationsDeferred = async {
                    fetchAllPages(site, modifiedAfterGmt, pageSize, maxPages)
                }
                val trashVariationsDeferred = async {
                    if (isFullSync) {
                        // We run incremental sync right after completing full sync -> no need to fetch trash products
                        emptyList()
                    } else {
                        fetchAllTrashVariations(site, pageSize)
                    }
                }

                val (variations, serverDate) = regularVariationsDeferred.await()
                val trashVariations = trashVariationsDeferred.await()
                Triple(variations, trashVariations, serverDate)
            }

            val allVariations = variations + trashVariations

            posLocalCatalogStore.executeInTransaction {
                if (isFullSync) {
                    posLocalCatalogStore.deleteAllVariations(
                        siteId = site.localId()
                    ).getOrThrow()
                }

                posLocalCatalogStore.upsertVariations(allVariations).getOrThrow()
            }.fold(
                onSuccess = {
                    logger.d("Local Catalog variations transaction committed successfully")
                    WooPosSyncVariationsResult.Success(allVariations.size, serverDate)
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
                    is CatalogTooLargeException -> error.toSyncResult()
                    else -> WooPosSyncVariationsResult.Failed.UnexpectedError(
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
        var currentPage = 1
        var pagesSynced = 0
        var totalPages = maxPages
        var firstPageServerDate: String? = null

        val variations = mutableListOf<WooPosVariationEntity>()

        while (pagesSynced < totalPages) {
            val result = posLocalCatalogStore.fetchRecentlyModifiedVariations(
                site = site,
                modifiedAfterGmt = modifiedAfterGmt,
                page = currentPage,
                pageSize = pageSize,
            )

            result.fold(
                onSuccess = { syncResult ->
                    processPageResult(syncResult, pagesSynced, maxPages)
                    if (pagesSynced == 0) {
                        firstPageServerDate = syncResult.serverDate
                    }
                    variations.addAll(syncResult.variations)
                    totalPages = syncResult.totalPages
                    pagesSynced++

                    if (!syncResult.hasMore || syncResult.syncedCount == 0) {
                        logger.d("Local Catalog: No more variations to sync")
                    } else {
                        currentPage = syncResult.nextPage
                    }
                },
                onFailure = { error ->
                    logger.e("Local Catalog Variations sync failed on page ${pagesSynced + 1}: ${error.message}")
                    throw error
                }
            )
        }

        logger.d("Local catalog variations sync completed, variations synced across $pagesSynced pages")
        return Pair(
            variations.toList(),
            requireNotNull(firstPageServerDate) { "Can't be null since we throw an exception in the store layer." }
        )
    }

    private fun processPageResult(
        syncResult: WooPosVariationsFetchResult,
        pagesSynced: Int,
        maxPages: Int
    ) {
        if (pagesSynced == 0) {
            if (syncResult.totalPages > maxPages) {
                logger.e(
                    "Local Catalog variations too large: ${syncResult.totalPages} pages exceed maximum " +
                        "of $maxPages pages"
                )
                throw CatalogTooLargeException(syncResult.totalPages, maxPages)
            }
        }
        logger.d("Local Catalog variations page ${pagesSynced + 1} synced, ${syncResult.syncedCount} variations")
    }

    private fun handleTransactionError(error: Throwable): WooPosSyncVariationsResult {
        return when (error) {
            is CatalogTooLargeException -> {
                logger.e("Local Catalog variations too large, transaction rolled back")
                error.toSyncResult()
            }

            else -> {
                logger.e("Local Catalog Variations Transaction failed and was rolled back: ${error.message}")
                WooPosSyncVariationsResult.Failed.UnexpectedError(
                    error.message ?: "Local Catalog Variations Transaction failed and was rolled back"
                )
            }
        }
    }

    private suspend fun fetchAllTrashVariations(
        site: SiteModel,
        pageSize: Int
    ): List<WooPosVariationEntity> {
        var currentPage = 1
        var pagesSynced = 0
        val trashVariations = mutableListOf<WooPosVariationEntity>()

        logger.d("Fetching all trash variations for incremental sync")

        while (true) {
            val result = posLocalCatalogStore.fetchRecentlyModifiedVariations(
                site = site,
                modifiedAfterGmt = null,
                page = currentPage,
                pageSize = pageSize,
                includeStatus = listOf(CoreProductStatus.TRASH)
            )

            result.fold(
                onSuccess = { syncResult ->
                    trashVariations.addAll(syncResult.variations)
                    pagesSynced++

                    if (!syncResult.hasMore || syncResult.syncedCount == 0) {
                        logger.d(
                            "Finished fetching trash variations: ${trashVariations.size} " +
                                "total across $pagesSynced pages"
                        )
                        break
                    } else {
                        currentPage = syncResult.nextPage
                    }
                },
                onFailure = { error ->
                    logger.e("Failed to fetch trash variations on page ${pagesSynced + 1}: ${error.message}")
                    throw error
                }
            )
        }

        return trashVariations.toList()
    }

    internal class CatalogTooLargeException(
        val totalPages: Int,
        val maxPages: Int
    ) : Exception("Local Catalog variations too large: $totalPages pages exceed maximum of $maxPages pages") {
        fun toSyncResult(): WooPosSyncVariationsResult.Failed.CatalogTooLarge {
            return WooPosSyncVariationsResult.Failed.CatalogTooLarge(totalPages, maxPages)
        }
    }
}
