package com.woocommerce.android.ui.woopos.localcatalog

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.LocalCatalogSyncSkipped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncSkipReason
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Duration.Companion.milliseconds

@HiltWorker
class WooPosLocalCatalogSyncWorker
@AssistedInject
@Suppress("LongParameterList")
constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val selectedSite: SelectedSite,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val syncRepository: WooPosLocalCatalogSyncRepository,
    private val logger: WooPosLogWrapper,
    private val timeProvider: DateTimeProvider,
    private val syncStatusChecker: WooPosFullSyncStatusChecker,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val syncTimestampManager: WooPosSyncTimestampManager,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "PosLocalCatalogSyncWork"
        private const val DAYS_SINCE_LAST_USE_THRESHOLD = 30L
    }

    override suspend fun doWork(): Result {
        if (isPosInactive()) {
            logger.d("POS has been inactive recently, skipping local catalog sync")
            trackSyncSkipped(SyncSkipReason.POS_NOT_OPENED_30_DAYS)
            return Result.success()
        }

        validateSyncStatus()?.let { return it }

        val site = selectedSite.getOrNull() ?: run {
            logger.e("Unexpected: Site is null after validation")
            trackSyncSkipped(SyncSkipReason.SITE_NOT_SELECTED)
            return Result.failure()
        }

        logger.d("Starting FULL local catalog sync")

        val fullSyncResult = syncRepository.syncLocalCatalogFull(site)

        return when (fullSyncResult) {
            is PosLocalCatalogSyncResult.Success -> {
                logger.d(
                    "Local catalog FULL sync completed successfully. Products: ${fullSyncResult.productsSynced}, " +
                        "Variations: ${fullSyncResult.variationsSynced}, Duration: ${fullSyncResult.syncDurationMs}ms"
                )
                logger.d("Starting Local catalog INCREMENTAL sync.")
                val incrementalSyncResult = syncRepository.syncLocalCatalogIncremental(site)
                if (incrementalSyncResult is PosLocalCatalogSyncResult.Failure) {
                    logger.d(
                        "Local catalog INCREMENTAL sync failed."
                    )
                }
                Result.success()
            }

            is PosLocalCatalogSyncResult.Failure -> {
                logger.e("Local catalog FULL sync failed: ${fullSyncResult.error}. Retrying ...")
                Result.retry()
            }
        }
    }

    private suspend fun validateSyncStatus(): Result? {
        val syncRequirement = syncStatusChecker.checkSyncRequirement()
        return when (syncRequirement) {
            is WooPosFullSyncRequirement.NotRequired -> {
                logger.d(
                    "Local catalog sync NotRequired: Catalog was last synced at " +
                        "${syncRequirement.lastSyncTimestamp}"
                )
                trackSyncSkipped(SyncSkipReason.SYNC_NOT_REQUIRED)
                Result.success()
            }
            is WooPosFullSyncRequirement.LocalCatalogDisabled -> {
                logger.d("Local catalog sync LocalCatalogDisabled: ${syncRequirement.message}")
                trackSyncSkipped(SyncSkipReason.LOCAL_CATALOG_DISABLED)
                Result.success()
            }
            is WooPosFullSyncRequirement.Error -> {
                logger.e("Sync requirement check failed: ${syncRequirement.message}. Retrying...")
                trackSyncSkipped(SyncSkipReason.CHECKING_SYNC_REQUIREMENT_FAILED)
                Result.retry()
            }
            is WooPosFullSyncRequirement.BlockingRequired,
            is WooPosFullSyncRequirement.NonBlockingRequired -> {
                logger.d("Proceeding with sync (requirement: $syncRequirement)")
                null
            }
        }
    }

    private suspend fun isPosInactive(): Boolean {
        val lastUsedTimestamp = preferencesRepository.getLastUsedTimestamp()
            ?: return hasAlreadyCompletedFullSync()
        val daysSinceLastUse = (timeProvider.now() - lastUsedTimestamp).milliseconds.inWholeDays
        return if (daysSinceLastUse > DAYS_SINCE_LAST_USE_THRESHOLD) {
            logger.d(
                "POS not used in the last $DAYS_SINCE_LAST_USE_THRESHOLD days " +
                    "(last used $daysSinceLastUse days ago), skipping background full catalog sync."
            )
            true
        } else {
            false
        }
    }

    private suspend fun hasAlreadyCompletedFullSync(): Boolean {
        val alreadySynced = syncTimestampManager.getFullSyncLastCompletedTimestamp() != null
        if (alreadySynced) {
            logger.d(
                "POS was never opened and a full catalog sync has already completed, " +
                    "skipping background full catalog sync."
            )
        }
        return alreadySynced
    }

    private suspend fun trackSyncSkipped(skipReason: SyncSkipReason) {
        analyticsTracker.track(
            LocalCatalogSyncSkipped(
                syncType = SyncType.FULL,
                skipReason = skipReason
            )
        )
    }
}
