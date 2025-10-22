package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
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
    private val prefsRepo: WooPosPreferencesRepository,
    private val checkCatalogSizeAction: WooPosCheckCatalogSizeAction,
    private val getWooVersion: GetWooCorePluginCachedVersion,
    private val wooPosLogWrapper: WooPosLogWrapper
) {
    @Suppress("ReturnCount")
    suspend fun checkSyncRequirement(): WooPosFullSyncRequirement {
        if (!wooPosLocalCatalogM1Enabled()) {
            wooPosLogWrapper.d("Full sync check skipped: Local catalog feature not enabled")
            return WooPosFullSyncRequirement.LocalCatalogDisabled("Local catalog feature not enabled")
        }

        if(!isVariationsEndpointAvailable()) {
            wooPosLogWrapper.d("Full sync check skipped: Variations endpoint not available")
            return WooPosFullSyncRequirement.LocalCatalogDisabled("Variations endpoint not available")
        }

        val site = selectedSite.getOrNull()
        if (site == null) {
            wooPosLogWrapper.e("Full sync check failed: No site selected")
            error("No site selected")
        }

        if (!prefsRepo.isPeriodicSyncEnabledForSite(site.siteId)) {
            wooPosLogWrapper.d("Full sync check skipped: Periodic Sync disabled for site.")
            return WooPosFullSyncRequirement.LocalCatalogDisabled("Periodic Sync disabled for site.")
        }

        if (localCatalogStore.getProductCount(site.localId()).getOrNull() == 0) {
            val size = checkCatalogSizeAction
                .execute(site, maxTotalItems = WooPosLocalCatalogSyncRepository.MAX_TOTAL_ITEMS_FULL_SYNC)

            if (size is WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult.CatalogTooLarge) {
                prefsRepo.disablePeriodicSyncForSite(site.siteId)
                return WooPosFullSyncRequirement.LocalCatalogDisabled("Catalog too large - ${size.error}")
            }
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
        private const val WC_VARIATIONS_ENDPOINT_AVAILABLE = "10.3.0"
    }

    fun isVariationsEndpointAvailable(): Boolean {
        val currentWooCoreVersion = getWooVersion() ?: return false

        return currentWooCoreVersion.semverCompareTo(WC_VARIATIONS_ENDPOINT_AVAILABLE) >= 0
    }
}

sealed class WooPosFullSyncRequirement {
    data object NotRequired : WooPosFullSyncRequirement()
    data object Overdue : WooPosFullSyncRequirement()
    data object BlockingRequired : WooPosFullSyncRequirement()
    data class Error(val message: String) : WooPosFullSyncRequirement()
    data class LocalCatalogDisabled(val message: String) : WooPosFullSyncRequirement()
}
