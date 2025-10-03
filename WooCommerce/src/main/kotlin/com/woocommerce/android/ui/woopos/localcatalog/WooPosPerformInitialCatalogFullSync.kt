package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

sealed class WooPosFullSyncStatus {
    data object NotRequired : WooPosFullSyncStatus()
    data object InProgress : WooPosFullSyncStatus()
    data object Success : WooPosFullSyncStatus()
    data class Failed(val error: String) : WooPosFullSyncStatus()
}

class WooPosPerformInitialCatalogFullSync @Inject constructor(
    private val syncRepository: WooPosLocalCatalogSyncRepository,
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val selectedSite: SelectedSite,
    private val networkStatus: WooPosNetworkStatus,
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
    private val syncScheduler: WooPosLocalCatalogSyncScheduler,
    private val localCatalogStore: WooPosLocalCatalogStore,
    private val wooPosLogWrapper: WooPosLogWrapper
) {
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    operator fun invoke(): Flow<WooPosFullSyncStatus> = flow {
        if (!wooPosLocalCatalogM1Enabled()) {
            wooPosLogWrapper.d("Full sync check skipped: Local catalog feature not enabled")
            emit(WooPosFullSyncStatus.NotRequired)
            return@flow
        }

        val site = selectedSite.getOrNull()
        if (site == null) {
            wooPosLogWrapper.d("Full sync check skipped: No site selected")
            emit(WooPosFullSyncStatus.NotRequired)
            return@flow
        }

        if (!networkStatus.isConnected()) {
            wooPosLogWrapper.d("Full sync check skipped: No network connection")
            emit(WooPosFullSyncStatus.Failed("No network connection"))
            return@flow
        }

        val lastFullSyncTimestamp = syncTimestampManager.getFullSyncLastCompletedTimestamp()
        val productCount = localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(site.id))
            .getOrElse {
                wooPosLogWrapper.e("Failed to get product count: ${it.message}")
                0
            }
        val catalogIsEmpty = productCount == 0

        val syncRequired = when {
            lastFullSyncTimestamp == null -> {
                wooPosLogWrapper.d("Full sync required: Never synced before")
                SyncRequirement.BlockingRequired
            }
            catalogIsEmpty -> {
                wooPosLogWrapper.d("Full sync required: Catalog is empty")
                SyncRequirement.BlockingRequired
            }
            isFullSyncOverdue(lastFullSyncTimestamp) -> {
                wooPosLogWrapper.d("Full sync required: Overdue (last sync: $lastFullSyncTimestamp)")
                SyncRequirement.BackgroundRequired
            }
            else -> {
                wooPosLogWrapper.d("Full sync not required: Recent sync found at $lastFullSyncTimestamp")
                SyncRequirement.NotRequired
            }
        }

        when (syncRequired) {
            SyncRequirement.NotRequired -> {
                emit(WooPosFullSyncStatus.NotRequired)
            }
            SyncRequirement.BackgroundRequired -> {
                wooPosLogWrapper.d("Triggering overdue full sync in background")
                syncScheduler.triggerManualFullCatalogSync()
                emit(WooPosFullSyncStatus.NotRequired)
            }
            SyncRequirement.BlockingRequired -> {
                if (syncScheduler.isOneTimeWorkRunning()) {
                    monitorWorkerProgress()
                } else {
                    performBlockingSync(site)
                }
            }
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

    private suspend fun FlowCollector<WooPosFullSyncStatus>.performBlockingSync(site: SiteModel) {
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

    private fun isFullSyncOverdue(lastSyncTimestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSync = currentTime - lastSyncTimestamp
        val overdueThreshold = FULL_SYNC_OVERDUE_THRESHOLD.inWholeMilliseconds
        return timeSinceLastSync >= overdueThreshold
    }

    private enum class SyncRequirement {
        NotRequired,
        BackgroundRequired,
        BlockingRequired
    }

    companion object {
        private const val WORKER_STATUS_CHECK_INTERVAL_MS = 1000L
        private val FULL_SYNC_OVERDUE_THRESHOLD = 24.hours
    }
}
