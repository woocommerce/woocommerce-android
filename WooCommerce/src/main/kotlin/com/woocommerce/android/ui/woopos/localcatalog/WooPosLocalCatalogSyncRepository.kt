package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosLocalCatalogSyncRepository @Inject constructor(
    private val posSyncAction: WooPosSyncAction,
    private val posCheckCatalogSizeAction: WooPosCheckCatalogSizeAction,
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val dispatchers: CoroutineDispatchers,
    private val logger: WooPosLogWrapper,
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val dateTimeProvider: DateTimeProvider,
) {
    companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES_PER_FULL_SYNC = 10
        const val MAX_PAGES_PER_INCREMENTAL_SYNC = 3
        const val MAX_TOTAL_ITEMS_FULL_SYNC = 1000
        const val MAX_TOTAL_ITEMS_INCREMENTAL_SYNC = 300
    }

    suspend fun syncLocalCatalogFull(site: SiteModel): PosLocalCatalogSyncResult = withContext(dispatchers.io) {
        return@withContext performSync(
            site = site,
            pageSize = PAGE_SIZE,
            maxPages = MAX_PAGES_PER_FULL_SYNC,
            maxTotalItems = MAX_TOTAL_ITEMS_FULL_SYNC
        ).also {
            if (it is PosLocalCatalogSyncResult.Success) {
                syncTimestampManager.storeFullSyncLastCompletedTimestamp(dateTimeProvider.now())
            }
            if (it is PosLocalCatalogSyncResult.Failure.CatalogTooLarge) {
                preferencesRepository.disablePeriodicSyncForSite(site.localId())
            }
        }
    }

    suspend fun syncLocalCatalogIncremental(site: SiteModel): PosLocalCatalogSyncResult = withContext(dispatchers.io) {
        val lastSyncTimestamp = syncTimestampManager.getProductsLastSyncTimestamp() ?: 0L
        val modifiedAfterGmt = syncTimestampManager.formatTimestampForApi(lastSyncTimestamp)

        return@withContext performSync(
            site = site,
            modifiedAfterGmt = modifiedAfterGmt,
            pageSize = PAGE_SIZE,
            maxPages = MAX_PAGES_PER_INCREMENTAL_SYNC,
            maxTotalItems = MAX_TOTAL_ITEMS_INCREMENTAL_SYNC
        )
    }

    @Suppress("ReturnCount")
    private suspend fun performSync(
        site: SiteModel,
        pageSize: Int,
        maxPages: Int,
        maxTotalItems: Int,
        modifiedAfterGmt: String? = null,
    ): PosLocalCatalogSyncResult {
        val startTime = dateTimeProvider.now()

        logger.d("Starting sync for items modified after $modifiedAfterGmt, max pages: $maxPages")

        val catalogSizeCheckResult = posCheckCatalogSizeAction.execute(
            site = site,
            modifiedAfterGmt = modifiedAfterGmt,
            maxTotalItems = maxTotalItems
        )
        if (catalogSizeCheckResult is WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult.CatalogTooLarge) {
            return catalogSizeCheckResult.toPosLocalCatalogSyncFailure()
        }

        val syncResult = posSyncAction.syncCatalog(site, modifiedAfterGmt, pageSize, maxPages)
        if (syncResult is WooPosSyncResult.Failed) {
            return syncResult.toPosLocalCatalogSyncFailure()
        }

        val successResult = syncResult as WooPosSyncResult.Success

        successResult.productsServerDate?.let { serverDate ->
            syncTimestampManager.parseTimestampFromApi(serverDate)?.let { timestamp ->
                syncTimestampManager.storeProductsLastSyncTimestamp(timestamp)
                logger.d("Stored products sync timestamp: $serverDate")
            }
        }

        successResult.variationsServerDate?.let { serverDate ->
            syncTimestampManager.parseTimestampFromApi(serverDate)?.let { timestamp ->
                syncTimestampManager.storeVariationsLastSyncTimestamp(timestamp)
                logger.d("Stored variations sync timestamp: $serverDate")
            }
        }

        val syncDuration = dateTimeProvider.now() - startTime

        return PosLocalCatalogSyncResult.Success(
            productsSynced = successResult.productsSynced,
            variationsSynced = successResult.variationsSynced,
            syncDurationMs = syncDuration
        )
    }

    suspend fun getProductCount(site: SiteModel): Int =
        posLocalCatalogStore.getProductCount(LocalId(site.id)).getOrElse { 0 }

    suspend fun getVariationCount(site: SiteModel): Int =
        posLocalCatalogStore.getVariationCount(LocalId(site.id)).getOrElse { 0 }
}

private fun WooPosSyncResult.Failed.toPosLocalCatalogSyncFailure(): PosLocalCatalogSyncResult.Failure {
    return when (this) {
        is WooPosSyncResult.Failed.CatalogTooLarge -> {
            PosLocalCatalogSyncResult.Failure.CatalogTooLarge(
                error = "Catalog too large: $totalPages pages exceed maximum of $maxPages pages",
            )
        }

        is WooPosSyncResult.Failed.UnexpectedError -> {
            PosLocalCatalogSyncResult.Failure.UnexpectedError(errorMessage)
        }
    }
}

private fun WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult.CatalogTooLarge.toPosLocalCatalogSyncFailure():
    PosLocalCatalogSyncResult.Failure {
    return PosLocalCatalogSyncResult.Failure.CatalogTooLarge(
        error = error,
    )
}
