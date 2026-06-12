package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosConnectionTypeProvider
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.LocalCatalogSyncCompleted
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.LocalCatalogSyncFailed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.LocalCatalogSyncStarted
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncErrorType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosLocalCatalogSyncRepository @Inject constructor(
    private val posSyncAction: WooPosSyncAction,
    private val posFileBasedSyncAction: WooPosFileBasedSyncAction,
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val dispatchers: CoroutineDispatchers,
    private val logger: WooPosLogWrapper,
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val dateTimeProvider: DateTimeProvider,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val connectionTypeProvider: WooPosConnectionTypeProvider,
) {
    val syncState: StateFlow<WooPosFileBasedSyncAction.SyncState?> = posFileBasedSyncAction.syncState

    companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES_PER_INCREMENTAL_SYNC = Int.MAX_VALUE
    }

    suspend fun syncLocalCatalogFull(site: SiteModel): PosLocalCatalogSyncResult = withContext(dispatchers.io) {
        analyticsTracker.track(
            LocalCatalogSyncStarted(
                syncType = SyncType.FULL,
                connectionType = connectionTypeProvider.getConnectionType()
            )
        )

        return@withContext performFileBasedSync(site).also { result ->
            when (result) {
                is PosLocalCatalogSyncResult.Success -> {
                    syncTimestampManager.storeFullSyncLastCompletedTimestamp(dateTimeProvider.now())
                    trackSyncCompleted(site, SyncType.FULL, result)
                }
                is PosLocalCatalogSyncResult.Failure -> {
                    trackSyncFailed(SyncType.FULL, result)
                }
            }
        }
    }

    suspend fun syncLocalCatalogIncremental(site: SiteModel): PosLocalCatalogSyncResult = withContext(dispatchers.io) {
        analyticsTracker.track(
            LocalCatalogSyncStarted(
                syncType = SyncType.INCREMENTAL,
                connectionType = connectionTypeProvider.getConnectionType()
            )
        )

        val lastSyncTimestamp = syncTimestampManager.getProductsLastSyncTimestamp() ?: 0L
        val modifiedAfterGmt = syncTimestampManager.formatTimestampForApi(lastSyncTimestamp)

        return@withContext performSync(
            site = site,
            modifiedAfterGmt = modifiedAfterGmt,
            pageSize = PAGE_SIZE,
            maxPages = MAX_PAGES_PER_INCREMENTAL_SYNC,
        ).also { result ->
            when (result) {
                is PosLocalCatalogSyncResult.Success -> {
                    trackSyncCompleted(site, SyncType.INCREMENTAL, result)
                }
                is PosLocalCatalogSyncResult.Failure -> {
                    trackSyncFailed(SyncType.INCREMENTAL, result)
                }
            }
        }
    }

    private suspend fun performFileBasedSync(site: SiteModel): PosLocalCatalogSyncResult {
        return when (val result = posFileBasedSyncAction.syncCatalog(site)) {
            is WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success -> result.result.also {
                if (result.lastModifiedDate != null) {
                    syncTimestampManager.parseTimestampFromApi(result.lastModifiedDate)?.let { timestamp ->
                        syncTimestampManager.storeProductsLastSyncTimestamp(timestamp)
                        syncTimestampManager.storeVariationsLastSyncTimestamp(timestamp)
                    }
                }
            }

            is WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Failure -> result.result
        }
    }

    private suspend fun trackSyncCompleted(
        site: SiteModel,
        syncType: SyncType,
        result: PosLocalCatalogSyncResult.Success
    ) {
        val totalProducts = posLocalCatalogStore.getProductCount(site.localId()).getOrElse { 0 }
        val totalVariations = posLocalCatalogStore.getVariationCount(site.localId()).getOrElse { 0 }

        analyticsTracker.track(
            LocalCatalogSyncCompleted(
                syncType = syncType,
                productsSynced = result.productsSynced,
                variationsSynced = result.variationsSynced,
                totalProducts = totalProducts,
                totalVariations = totalVariations,
                syncDurationMs = result.syncDurationMs,
                generationDurationMs = result.generationDurationMs,
                pollAttempts = result.pollAttempts
            )
        )
    }

    private suspend fun trackSyncFailed(
        syncType: SyncType,
        result: PosLocalCatalogSyncResult.Failure
    ) {
        val errorType = when (result) {
            is PosLocalCatalogSyncResult.Failure.NetworkError -> SyncErrorType.NETWORK_ERROR
            is PosLocalCatalogSyncResult.Failure.DatabaseError -> SyncErrorType.DATABASE_ERROR
            is PosLocalCatalogSyncResult.Failure.InvalidResponse -> SyncErrorType.INVALID_RESPONSE
            is PosLocalCatalogSyncResult.Failure.UnexpectedError -> SyncErrorType.UNEXPECTED_ERROR
            is PosLocalCatalogSyncResult.Failure.CatalogGenerationTimeout -> SyncErrorType.CATALOG_GENERATION_TIMEOUT
            is PosLocalCatalogSyncResult.Failure.CatalogFileBlocked -> SyncErrorType.CATALOG_FILE_BLOCKED
        }

        analyticsTracker.track(
            LocalCatalogSyncFailed(
                syncType = syncType,
                errorContext = "WooPosLocalCatalogSyncRepository",
                errorType = errorType,
                errorDescription = result.error.ifBlank { "Unknown error (${result::class.simpleName})" },
                lastGenerationState = result.lastGenerationState,
                pollAttempts = result.pollAttempts
            )
        )
    }

    private suspend fun performSync(
        site: SiteModel,
        pageSize: Int,
        maxPages: Int,
        modifiedAfterGmt: String? = null,
    ): PosLocalCatalogSyncResult {
        val startTime = dateTimeProvider.now()

        logger.d("Starting sync for items modified after $modifiedAfterGmt, max pages: $maxPages")

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
            PosLocalCatalogSyncResult.Failure.UnexpectedError(
                error = "Catalog too large: $totalPages pages exceed maximum of $maxPages pages",
            )
        }

        is WooPosSyncResult.Failed.NetworkError -> {
            PosLocalCatalogSyncResult.Failure.NetworkError(error)
        }

        is WooPosSyncResult.Failed.DatabaseError -> {
            PosLocalCatalogSyncResult.Failure.DatabaseError(error)
        }

        is WooPosSyncResult.Failed.InvalidResponse -> {
            PosLocalCatalogSyncResult.Failure.InvalidResponse(error)
        }

        is WooPosSyncResult.Failed.UnexpectedError -> {
            PosLocalCatalogSyncResult.Failure.UnexpectedError(error)
        }
    }
}
