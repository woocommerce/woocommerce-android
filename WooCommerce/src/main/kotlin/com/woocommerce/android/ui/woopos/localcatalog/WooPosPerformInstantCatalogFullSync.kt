package com.woocommerce.android.ui.woopos.localcatalog

import androidx.work.WorkInfo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WooPosPerformInstantCatalogFullSync @Inject constructor(
    private val syncRepository: WooPosLocalCatalogSyncRepository,
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val syncScheduler: WooPosLocalCatalogSyncScheduler,
    private val selectedSite: SelectedSite,
    private val wooPosLogWrapper: WooPosLogWrapper
) {
    suspend operator fun invoke(): Result<Unit> {
        val isOneTimeRunning = syncScheduler.observeOneTimeWorkStatus().first()
        val isPeriodicRunning = syncScheduler.observePeriodicWorkStatus().first()

        return when {
            isOneTimeRunning -> {
                wooPosLogWrapper.d("One-time worker is running, monitoring its progress")
                monitorWorkerProgress(syncScheduler.observeOneTimeWorkInfo(), "One-time")
            }
            isPeriodicRunning -> {
                wooPosLogWrapper.d("Periodic worker is running, monitoring its progress")
                monitorWorkerProgress(syncScheduler.observePeriodicWorkInfo(), "Periodic")
            }
            else -> {
                performBlockingSync()
            }
        }
    }

    private suspend fun monitorWorkerProgress(
        workInfoFlow: Flow<WorkInfo?>,
        workerType: String
    ): Result<Unit> {
        // WorkManager can stop a RUNNING worker (e.g., when the network constraint is no longer met
        // during a large catalog sync), transitioning it back to ENQUEUED/BLOCKED without reaching a
        // finished state. Waiting only for `isFinished` would hang in that case, so we also break out
        // whenever the worker leaves the RUNNING state.
        val finalWorkInfo = workInfoFlow
            .filter { workInfo ->
                workInfo == null || workInfo.state != WorkInfo.State.RUNNING
            }
            .first()

        return handleWorkerCompletion(finalWorkInfo, workerType)
    }

    private suspend fun handleWorkerCompletion(
        workInfo: WorkInfo?,
        workerType: String
    ): Result<Unit> {
        return when (workInfo?.state) {
            WorkInfo.State.SUCCEEDED -> {
                verifyAndEmitSyncCompletion(workerType, "worker completed successfully")
            }
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                wooPosLogWrapper.e("$workerType worker failed or cancelled: ${workInfo.state}")
                Result.failure(Exception("Background sync worker ${workInfo.state}"))
            }
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                wooPosLogWrapper.e(
                    "$workerType worker was stopped before finishing (state=${workInfo.state})"
                )
                Result.failure(Exception("Background sync worker was stopped: ${workInfo.state}"))
            }
            null -> {
                verifyAndEmitSyncCompletion(
                    workerType,
                    "worker info is null but sync timestamp found - assuming success"
                )
            }
            else -> {
                wooPosLogWrapper.e("$workerType worker finished with unexpected state: ${workInfo.state}")
                Result.failure(Exception("Unexpected worker state: ${workInfo.state}"))
            }
        }
    }

    private suspend fun verifyAndEmitSyncCompletion(
        workerType: String,
        successMessage: String
    ): Result<Unit> {
        val completedTimestamp = syncTimestampManager.getFullSyncLastCompletedTimestamp()
        return if (completedTimestamp != null) {
            wooPosLogWrapper.d("$workerType: $successMessage")
            Result.success(Unit)
        } else {
            wooPosLogWrapper.e("$workerType: Worker completed but no timestamp found")
            Result.failure(Exception("Worker completed but sync not verified"))
        }
    }

    private suspend fun performBlockingSync(): Result<Unit> {
        val site = selectedSite.getOrNull()
        if (site == null) {
            wooPosLogWrapper.e("Cannot perform blocking sync: No site selected")
            return Result.failure(Exception("No site selected"))
        }

        wooPosLogWrapper.d("Starting blocking full sync")

        val syncResult = syncRepository.syncLocalCatalogFull(site)
        return when (syncResult) {
            is PosLocalCatalogSyncResult.Success -> {
                wooPosLogWrapper.d(
                    "Blocking full sync completed successfully: " +
                        "${syncResult.productsSynced} products, " +
                        "${syncResult.variationsSynced} variations synced " +
                        "in ${syncResult.syncDurationMs}ms"
                )
                Result.success(Unit)
            }
            is PosLocalCatalogSyncResult.Failure -> {
                wooPosLogWrapper.e("Blocking full sync failed: ${syncResult.error}")
                Result.failure(Exception(syncResult.error))
            }
        }
    }
}
