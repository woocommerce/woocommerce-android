package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class WooPosFullSyncStatusChecker @Inject constructor(
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val selectedSite: SelectedSite,
    private val networkStatus: WooPosNetworkStatus,
    private val localCatalogStore: WooPosLocalCatalogStore,
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported,
    private val wooPosLogWrapper: WooPosLogWrapper,
    private val time: DateTimeProvider,
) {
    @Suppress("ReturnCount")
    suspend fun checkSyncRequirement(): WooPosFullSyncRequirement {
        val site = selectedSite.getOrNull()
        if (site == null) {
            wooPosLogWrapper.e("Full sync check failed: No site selected")
            return WooPosFullSyncRequirement.Error("No site selected")
        }

        if (!isLocalCatalogSupported()) {
            wooPosLogWrapper.d("Full sync check skipped: Local catalog not supported for site")
            return WooPosFullSyncRequirement.LocalCatalogDisabled("Local catalog not supported for site")
        }

        val productCount = localCatalogStore.getProductCount(site.localId()).getOrElse { 0 }

        val lastFullSyncTimestamp = syncTimestampManager.getFullSyncLastCompletedTimestamp()
        val catalogIsEmpty = productCount == 0

        if (lastFullSyncTimestamp == null) {
            if (!networkStatus.isConnected()) {
                wooPosLogWrapper.e("Cannot perform initial sync: No network connection")
                return WooPosFullSyncRequirement.Error("No network connection")
            }
            wooPosLogWrapper.d("Full sync required: Never synced before")
            return WooPosFullSyncRequirement.BlockingRequired
        }

        val now = time.now()
        val timeElapsedSinceLastSync = now - lastFullSyncTimestamp

        return when {
            timeElapsedSinceLastSync < FULL_SYNC_NOT_REQUIRED_THRESHOLD -> {
                wooPosLogWrapper.d(
                    "Full sync not required: Recent sync at $lastFullSyncTimestamp " +
                        "(${if (catalogIsEmpty) "empty catalog" else "$productCount products"})"
                )
                WooPosFullSyncRequirement.NotRequired(lastFullSyncTimestamp)
            }

            else -> {
                if (!networkStatus.isConnected()) {
                    wooPosLogWrapper.d(
                        "Full sync required but offline - allowing POS to load with cached data " +
                            "(${if (catalogIsEmpty) "empty catalog" else "$productCount products"})"
                    )
                }
                val isFullSyncOverdue = timeElapsedSinceLastSync > FULL_SYNC_OVERDUE_THRESHOLD
                if (isFullSyncOverdue) {
                    wooPosLogWrapper.d("Full sync overdue (last sync: $lastFullSyncTimestamp)")
                }
                WooPosFullSyncRequirement.NonBlockingRequired(lastFullSyncTimestamp, isFullSyncOverdue)
            }
        }
    }

    companion object {
        private val FULL_SYNC_OVERDUE_THRESHOLD = 7.days.inWholeMilliseconds
        private val FULL_SYNC_NOT_REQUIRED_THRESHOLD = 23.hours.inWholeMilliseconds
    }
}

sealed class WooPosFullSyncRequirement {
    data class NotRequired(val lastSyncTimestamp: Long) : WooPosFullSyncRequirement()
    data class NonBlockingRequired(
        val lastSyncTimestamp: Long,
        val isOverdue: Boolean
    ) : WooPosFullSyncRequirement()
    data object BlockingRequired : WooPosFullSyncRequirement()
    data class Error(val message: String) : WooPosFullSyncRequirement()
    data class LocalCatalogDisabled(val message: String) : WooPosFullSyncRequirement()
}
