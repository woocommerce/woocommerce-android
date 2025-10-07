package com.woocommerce.android.ui.woopos.localcatalog

import androidx.work.WorkInfo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class WooPosPerformInstantCatalogFullSync @Inject constructor(
    private val syncRepository: WooPosLocalCatalogSyncRepository,
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val syncScheduler: WooPosLocalCatalogSyncScheduler,
    private val selectedSite: SelectedSite,
    private val wooPosLogWrapper: WooPosLogWrapper
) {
    operator fun invoke(): Flow<WooPosFullSyncState> = flow {
        val isOneTimeRunning = syncScheduler.observeOneTimeWorkStatus().first()
        val isPeriodicRunning = syncScheduler.observePeriodicWorkStatus().first()

        when {
            isOneTimeRunning -> {
                wooPosLogWrapper.d("One-time worker is running, monitoring its progress")
                monitorOneTimeWorkerProgress()
            }
            isPeriodicRunning -> {
                wooPosLogWrapper.d("Periodic worker is running, monitoring its progress")
                monitorPeriodicWorkerProgress()
            }
            else -> {
                performBlockingSync()
            }
        }
    }

    private suspend fun FlowCollector<WooPosFullSyncState>.monitorOneTimeWorkerProgress() {
        emit(WooPosFullSyncState.InProgress)

        val finalWorkInfo = syncScheduler.observeOneTimeWorkInfo()
            .filter { workInfo ->
                workInfo?.state?.isFinished == true
            }
            .first()

        handleWorkerCompletion(finalWorkInfo, "One-time")
    }

    private suspend fun FlowCollector<WooPosFullSyncState>.monitorPeriodicWorkerProgress() {
        emit(WooPosFullSyncState.InProgress)

        val finalWorkInfo = syncScheduler.observePeriodicWorkInfo()
            .filter { workInfo ->
                workInfo?.state?.isFinished == true
            }
            .first()

        handleWorkerCompletion(finalWorkInfo, "Periodic")
    }

    private suspend fun FlowCollector<WooPosFullSyncState>.handleWorkerCompletion(
        workInfo: WorkInfo?,
        workerType: String
    ) {
        when (workInfo?.state) {
            WorkInfo.State.SUCCEEDED -> {
                val completedTimestamp = syncTimestampManager.getFullSyncLastCompletedTimestamp()
                if (completedTimestamp != null) {
                    wooPosLogWrapper.d("$workerType worker completed successfully")
                    emit(WooPosFullSyncState.Success)
                } else {
                    wooPosLogWrapper.e("$workerType worker succeeded but no timestamp found")
                    emit(WooPosFullSyncState.Failed("Worker succeeded but sync not verified"))
                }
            }
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                wooPosLogWrapper.e("$workerType worker failed or cancelled: ${workInfo.state}")
                emit(WooPosFullSyncState.Failed("Background sync worker ${workInfo.state}"))
            }
            else -> {
                wooPosLogWrapper.e("$workerType worker finished with unexpected state: ${workInfo?.state}")
                emit(WooPosFullSyncState.Failed("Unexpected worker state"))
            }
        }
    }

    private suspend fun FlowCollector<WooPosFullSyncState>.performBlockingSync() {
        val site = selectedSite.getOrNull()
        if (site == null) {
            wooPosLogWrapper.e("Cannot perform blocking sync: No site selected")
            emit(WooPosFullSyncState.Failed("No site selected"))
            return
        }

        wooPosLogWrapper.d("Starting blocking full sync")
        emit(WooPosFullSyncState.InProgress)

        val syncResult = syncRepository.syncLocalCatalogFull(site)
        when (syncResult) {
            is PosLocalCatalogSyncResult.Success -> {
                syncTimestampManager.storeFullSyncLastCompletedTimestamp(System.currentTimeMillis())
                wooPosLogWrapper.d(
                    "Blocking full sync completed successfully: " +
                        "${syncResult.productsSynced} products, " +
                        "${syncResult.variationsSynced} variations synced " +
                        "in ${syncResult.syncDurationMs}ms"
                )
                emit(WooPosFullSyncState.Success)
            }
            is PosLocalCatalogSyncResult.Failure -> {
                wooPosLogWrapper.e("Blocking full sync failed: ${syncResult.error}")
                emit(WooPosFullSyncState.Failed(syncResult.error))
            }
        }
    }
}

sealed class WooPosFullSyncState {
    data object InProgress : WooPosFullSyncState()
    data object Success : WooPosFullSyncState()
    data class Failed(val error: String) : WooPosFullSyncState()
}
