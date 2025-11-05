package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class WooPosFullSyncStatusChecker @Inject constructor(
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val selectedSite: SelectedSite,
    private val networkStatus: WooPosNetworkStatus,
    private val localCatalogStore: WooPosLocalCatalogStore,
    private val prefsRepo: WooPosPreferencesRepository,
    private val checkCatalogSizeAction: WooPosCheckCatalogSizeAction,
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported,
    private val wooPosLogWrapper: WooPosLogWrapper,
    private val time: DateTimeProvider,
) {
    @Suppress("ReturnCount")
    suspend fun checkSyncRequirement(): WooPosFullSyncRequirement {
        val site = selectedSite.getOrNull()
            ?: error("No site selected")

        if (!isLocalCatalogSupported(site.localId())) {
            wooPosLogWrapper.d("Full sync check skipped: Local catalog not supported for site")
            return WooPosFullSyncRequirement.LocalCatalogDisabled("Local catalog not supported for site")
        }

        if (localCatalogStore.getProductCount(site.localId()).getOrNull() == 0) {
            val size = checkCatalogSizeAction
                .execute(site, maxTotalItems = WooPosLocalCatalogSyncRepository.MAX_TOTAL_ITEMS_FULL_SYNC)

            if (size is WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult.CatalogTooLarge) {
                prefsRepo.disablePeriodicSyncForSite(site.localId())
                return WooPosFullSyncRequirement.LocalCatalogDisabled("Catalog too large - ${size.error}")
            }
        }

        val lastFullSyncTimestamp = syncTimestampManager.getFullSyncLastCompletedTimestamp()
        val productCount = localCatalogStore.getProductCount(LocalOrRemoteId.LocalId(site.id))
            .getOrElse { 0 }
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
