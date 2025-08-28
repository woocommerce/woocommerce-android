package com.woocommerce.android.ui.woopos.localcatalog

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PosLocalCatalogSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val accountRepository: AccountRepository,
    private val selectedSite: SelectedSite,
    private val syncRepository: PosLocalCatalogSyncRepository,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "PosLocalCatalogSyncWork"
    }

    override suspend fun doWork(): Result {
        if (!accountRepository.isUserLoggedIn()) {
            WooLog.d(T.POS, "User not logged in, skipping local catalog sync")
            return Result.failure()
        }

        val site = selectedSite.getOrNull()
        if (site == null) {
            WooLog.e(T.POS, "No selected WooCommerce site found, skipping local catalog sync")
            return Result.failure()
        }

        WooLog.d(T.POS, "Starting full local catalog sync")

        val syncResult = syncRepository.syncLocalCatalogFull(site)

        return when (syncResult) {
            is PosLocalCatalogSyncResult.Success -> {
                WooLog.d(
                    T.POS,
                    "Local catalog sync completed successfully. Products: ${syncResult.productsSynced}, " +
                        "Variations: ${syncResult.variationsSynced}, Duration: ${syncResult.syncDurationMs}ms"
                )
                Result.success()
            }

            is PosLocalCatalogSyncResult.Failure.UnexpectedError -> {
                WooLog.e(T.POS, "Local catalog sync failed: ${syncResult.error}. Retrying ...")
                Result.retry()
            }
            is PosLocalCatalogSyncResult.Failure.CatalogTooLarge -> {
                // TBD Local Catalog - stop future syncs for this site if catalog too large
                WooLog.e(T.POS, "Local catalog sync failed: ${syncResult.error}.")
                Result.failure()
            }
        }
    }
}
