package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
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

    @Suppress("ReturnCount")
    suspend fun execute(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
        pageSize: Int,
        maxPages: Int
    ): WooPosSyncVariationsResult {
        var currentPage = 1
        var pagesSynced = 0
        var totalSyncedVariations = 0
        var shouldContinue = true
        var lastServerDate: String? = null

        while (shouldContinue) {
            val result = posLocalCatalogStore.syncRecentlyModifiedVariations(
                site = site,
                modifiedAfterGmt = modifiedAfterGmt ?: "",
                page = currentPage,
                pageSize = pageSize
            )

            result.fold(
                onSuccess = { syncResult ->
                    // Check catalog size on first page
                    if (pagesSynced == 0) {
                        if (syncResult.totalPages > maxPages) {
                            logger.e(
                                "Variations catalog too large: ${syncResult.totalPages} pages " +
                                    "exceed maximum of $maxPages pages"
                            )
                            return WooPosSyncVariationsResult.Failed.CatalogTooLarge(syncResult.totalPages, maxPages)
                        }
                    }

                    logger.d("Variations page ${pagesSynced + 1} synced, ${syncResult.syncedCount} variations")
                    totalSyncedVariations += syncResult.syncedCount
                    pagesSynced++
                    lastServerDate = syncResult.serverDate

                    if (!syncResult.hasMore || syncResult.syncedCount == 0) {
                        logger.d("No more variations to sync")
                        shouldContinue = false
                    } else {
                        currentPage = syncResult.nextPage
                    }
                },
                onFailure = { error ->
                    logger.e("Variations sync failed on page ${pagesSynced + 1}: ${error.message}")
                    return WooPosSyncVariationsResult.Failed.UnexpectedError(error.message ?: "Unknown error")
                }
            )
        }

        logger.d("Variations sync completed, $totalSyncedVariations variations synced across $pagesSynced pages")
        return WooPosSyncVariationsResult.Success(totalSyncedVariations, lastServerDate)
    }
}
