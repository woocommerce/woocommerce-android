package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class WooPosFullSyncStatusChecker @Inject constructor(
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val selectedSite: SelectedSite,
    private val networkStatus: WooPosNetworkStatus,
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
    private val localCatalogStore: WooPosLocalCatalogStore,
    private val wooPosLogWrapper: WooPosLogWrapper
) {
    @Suppress("ReturnCount")
    suspend fun checkSyncRequirement(): WooPosFullSyncRequirement {
        if (!wooPosLocalCatalogM1Enabled()) {
            wooPosLogWrapper.d("Full sync check skipped: Local catalog feature not enabled")
            return WooPosFullSyncRequirement.NotRequired
        }

        // TBD local catalog: check woo version

        val site = selectedSite.getOrNull()
        if (site == null) {
            wooPosLogWrapper.e("Full sync check failed: No site selected")
            return WooPosFullSyncRequirement.Error("No site selected")
        }

        val lastFullSyncTimestamp = syncTimestampManager.getFullSyncLastCompletedTimestamp()
        val productCount = localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(site.id))
            .getOrElse {
                wooPosLogWrapper.e("Failed to get product count: ${it.message}")
                0
            }
        val catalogIsEmpty = productCount == 0

        if (lastFullSyncTimestamp == null) {
            if (!networkStatus.isConnected()) {
                wooPosLogWrapper.e("Cannot perform initial sync: No network connection")
                return WooPosFullSyncRequirement.Error("No network connection")
            }
            wooPosLogWrapper.d("Full sync required: Never synced before")
            return WooPosFullSyncRequirement.BlockingRequired
        }

        return when {
            isFullSyncOverdue(lastFullSyncTimestamp) -> {
                if (!networkStatus.isConnected()) {
                    wooPosLogWrapper.d(
                        "Full sync overdue but offline - allowing POS to load with cached data " +
                            "(${if (catalogIsEmpty) "empty catalog" else "$productCount products"})"
                    )
                }
                wooPosLogWrapper.d("Full sync overdue (last sync: $lastFullSyncTimestamp)")
                WooPosFullSyncRequirement.Overdue
            }
            else -> {
                wooPosLogWrapper.d(
                    "Full sync not required: Recent sync at $lastFullSyncTimestamp " +
                        "(${if (catalogIsEmpty) "empty catalog" else "$productCount products"})"
                )
                WooPosFullSyncRequirement.NotRequired
            }
        }
    }

    private fun isFullSyncOverdue(lastSyncTimestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSync = currentTime - lastSyncTimestamp
        val overdueThreshold = FULL_SYNC_OVERDUE_THRESHOLD.inWholeMilliseconds
        return timeSinceLastSync >= overdueThreshold
    }

    companion object {
        private val FULL_SYNC_OVERDUE_THRESHOLD = 7.days
    }
}

sealed class WooPosFullSyncRequirement {
    data object NotRequired : WooPosFullSyncRequirement()
    data object Overdue : WooPosFullSyncRequirement()
    data object BlockingRequired : WooPosFullSyncRequirement()
    data class Error(val message: String) : WooPosFullSyncRequirement()
}
