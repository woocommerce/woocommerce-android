package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

sealed class WooPosFullSyncStatus {
    data object InProgress : WooPosFullSyncStatus()
    data object Success : WooPosFullSyncStatus()
    data class Failed(val error: String) : WooPosFullSyncStatus()
}

class WooPosPerformInstantCatalogFullSync @Inject constructor(
    private val syncRepository: WooPosLocalCatalogSyncRepository,
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val syncScheduler: WooPosLocalCatalogSyncScheduler,
    private val selectedSite: SelectedSite,
    private val wooPosLogWrapper: WooPosLogWrapper
) {
    operator fun invoke(): Flow<WooPosFullSyncStatus> = flow {
        if (syncScheduler.isOneTimeWorkRunning()) {
            monitorWorkerProgress()
        } else {
            performBlockingSync()
        }
    }

    private suspend fun FlowCollector<WooPosFullSyncStatus>.monitorWorkerProgress() {
        wooPosLogWrapper.d("One-time full sync worker already running, monitoring progress")
        emit(WooPosFullSyncStatus.InProgress)

        var workerStillRunning = true
        while (workerStillRunning) {
            delay(WORKER_STATUS_CHECK_INTERVAL_MS)
            val completedTimestamp = syncTimestampManager.getFullSyncLastCompletedTimestamp()
            if (completedTimestamp != null) {
                wooPosLogWrapper.d("One-time worker completed successfully")
                emit(WooPosFullSyncStatus.Success)
                return
            }
            workerStillRunning = syncScheduler.isOneTimeWorkRunning()
            if (!workerStillRunning) {
                wooPosLogWrapper.e("One-time worker stopped without success")
                emit(WooPosFullSyncStatus.Failed("Background sync worker failed"))
                return
            }
        }
    }

    private suspend fun FlowCollector<WooPosFullSyncStatus>.performBlockingSync() {
        val site = selectedSite.getOrNull()
        if (site == null) {
            wooPosLogWrapper.e("Cannot perform blocking sync: No site selected")
            emit(WooPosFullSyncStatus.Failed("No site selected"))
            return
        }

        wooPosLogWrapper.d("Starting blocking full sync")
        emit(WooPosFullSyncStatus.InProgress)

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
                emit(WooPosFullSyncStatus.Success)
            }
            is PosLocalCatalogSyncResult.Failure -> {
                wooPosLogWrapper.e("Blocking full sync failed: ${syncResult.error}")
                emit(WooPosFullSyncStatus.Failed(syncResult.error))
            }
        }
    }

    companion object {
        private const val WORKER_STATUS_CHECK_INTERVAL_MS = 1000L
    }
}
