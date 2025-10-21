package com.woocommerce.android.ui.woopos.localcatalog

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WooPosLocalCatalogSyncWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val preconditionsChecker: WooPosLocalCatalogSyncPreconditionsChecker,
    private val syncRepository: WooPosLocalCatalogSyncRepository,
    private val logger: WooPosLogWrapper,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "PosLocalCatalogSyncWork"
    }

    override suspend fun doWork(): Result {
        val preconditionResult: WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult = preconditionsChecker.checkPreconditions()

        val site = when (preconditionResult) {
            is WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Skip -> {
                return preconditionResult.workerResult
            }
            is WooPosLocalCatalogSyncPreconditionsChecker.PreconditionResult.Proceed -> {
                preconditionResult.site
            }
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
}
