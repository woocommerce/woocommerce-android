package com.woocommerce.android.ui.woopos.localcatalog

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
@Suppress("LongParameterList")
class WooPosLocalCatalogSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val accountRepository: AccountRepository,
    private val selectedSite: SelectedSite,
    private val syncRepository: WooPosLocalCatalogSyncRepository,
    private val logger: WooPosLogWrapper,
    private val featureFlagM1Enabled: WooPosLocalCatalogM1Enabled,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "PosLocalCatalogSyncWork"
    }

    @Suppress("ReturnCount")
    override suspend fun doWork(): Result {
        if (!featureFlagM1Enabled.invoke()) {
            logger.d("Feature flag disabled, skipping local catalog sync")
            return Result.failure()
        }
        if (!accountRepository.isUserLoggedIn()) {
            logger.d("User not logged in, skipping local catalog sync")
            return Result.failure()
        }

        val site = selectedSite.getOrNull()
        if (site == null) {
            logger.e("No selected WooCommerce site found, skipping local catalog sync")
            return Result.failure()
        }

        logger.d("Starting full local catalog sync")

        val syncResult = syncRepository.syncLocalCatalogFull(site)

        return when (syncResult) {
            is PosLocalCatalogSyncResult.Success -> {
                logger.d(
                    "Local catalog sync completed successfully. Products: ${syncResult.productsSynced}, " +
                        "Variations: ${syncResult.variationsSynced}, Duration: ${syncResult.syncDurationMs}ms"
                )
                Result.success()
            }

            is PosLocalCatalogSyncResult.Failure.UnexpectedError -> {
                logger.e("Local catalog sync failed: ${syncResult.error}. Retrying ...")
                Result.retry()
            }
            is PosLocalCatalogSyncResult.Failure.CatalogTooLarge -> {
                // TBD Local Catalog - stop future syncs for this site if catalog too large
                logger.e("Local catalog sync failed: ${syncResult.error}.")
                Result.failure()
            }
        }
    }
}
