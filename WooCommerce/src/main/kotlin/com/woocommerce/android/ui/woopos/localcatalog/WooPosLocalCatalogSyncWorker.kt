package com.woocommerce.android.ui.woopos.localcatalog

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.tab.WooPosTabShouldBeVisible
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.wordpress.android.fluxc.model.SiteModel
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
    private val wooPosTabShouldBeVisible: WooPosTabShouldBeVisible,
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported,
    private val syncStatusChecker: WooPosFullSyncStatusChecker,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "PosLocalCatalogSyncWork"
        private const val DAYS_SINCE_LAST_USE_THRESHOLD = 30L
    }

    @Suppress("ReturnCount")
    override suspend fun doWork(): Result {
        val isPosTabAvailable = wooPosTabShouldBeVisible()
        if (isPosTabAvailable.isSuccess && isPosTabAvailable.getOrNull() == false) {
            logger.d("POS tab is not visible, skipping local catalog sync")
            return Result.success()
        }

        if (isPosInactive()) {
            logger.d("POS has been inactive recently, skipping local catalog sync")
            return Result.success()
        }

        checkSyncRequirement()?.let { return it }

        val site = isCatalogSyncSupported() ?: return Result.failure()

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

            is PosLocalCatalogSyncResult.Failure.UnexpectedError -> {
                logger.e("Local catalog FULL sync failed: ${fullSyncResult.error}. Retrying ...")
                Result.retry()
            }

            is PosLocalCatalogSyncResult.Failure.CatalogTooLarge -> {
                logger.e(
                    "Local catalog FULL sync failed: ${fullSyncResult.error}. Permanently " +
                        "disabling periodic sync for site ${site.url}."
                )
                Result.failure()
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun checkSyncRequirement(): Result? {
        val syncRequirement = syncStatusChecker.checkSyncRequirement()
        return when (syncRequirement) {
            is WooPosFullSyncRequirement.NotRequired -> {
                logger.d(
                    "Local catalog sync NotRequired: Catalog was last synced at " +
                        "${syncRequirement.lastSyncTimestamp}"
                )
                Result.success()
            }
            is WooPosFullSyncRequirement.LocalCatalogDisabled -> {
                logger.d("Local catalog sync LocalCatalogDisabled: ${syncRequirement.message}")
                Result.success()
            }
            is WooPosFullSyncRequirement.Error -> {
                logger.e("Sync requirement check failed: ${syncRequirement.message}. Retrying...")
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
        val lastUsedTimestamp = preferencesRepository.getLastUsedTimestamp() ?: return false
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

    @Suppress("ReturnCount")
    private suspend fun isCatalogSyncSupported(): SiteModel? {
        val site = selectedSite.getOrNull()
        if (site == null) {
            logger.w("No selected WooCommerce site found, skipping local catalog sync")
            return null
        }

        if (!isLocalCatalogSupported(site.localId())) {
            logger.d("Local catalog not supported for site")
            return null
        }

        return site
    }
}
