package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult.Failure
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult.Success
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.LocalCatalogSyncSkipped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncSkipReason
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.SyncType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class WooPosPerformLocalCatalogIncrementalSync @Inject constructor(
    private val localCatalogSyncRepository: WooPosLocalCatalogSyncRepository,
    private val selectedSite: SelectedSite,
    private val networkStatus: WooPosNetworkStatus,
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported,
    private val wooPosLogWrapper: WooPosLogWrapper,
    private val analyticsTracker: WooPosAnalyticsTracker,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    fun execute(reason: WooPosIncrementalSyncReason) {
        appCoroutineScope.launch {
            val reasonDescription = reason.description
            if (!networkStatus.isConnected()) {
                wooPosLogWrapper.d("Skipping sync $reasonDescription: No network connection")
                trackSyncSkipped(SyncSkipReason.CHECKING_SYNC_REQUIREMENT_FAILED)
                return@launch
            }

            val site = selectedSite.getOrNull()
            if (site == null) {
                wooPosLogWrapper.d("Skipping sync $reasonDescription: No site selected")
                trackSyncSkipped(SyncSkipReason.SITE_NOT_SELECTED)
                return@launch
            }

            if (!isLocalCatalogSupported()) {
                wooPosLogWrapper.d("Skipping sync $reasonDescription: Local catalog not supported for site")
                trackSyncSkipped(SyncSkipReason.LOCAL_CATALOG_DISABLED)
                return@launch
            }

            performSync(site, reasonDescription)
        }
    }

    private suspend fun performSync(site: SiteModel, reasonDescription: String) {
        wooPosLogWrapper.d("Starting incremental sync $reasonDescription")
        val syncResult = localCatalogSyncRepository.syncLocalCatalogIncremental(site)
        when (syncResult) {
            is Success -> {
                wooPosLogWrapper.d(
                    "Sync $reasonDescription completed successfully: " +
                        "${syncResult.productsSynced} products, " +
                        "${syncResult.variationsSynced} variations synced " +
                        "in ${syncResult.syncDurationMs}ms"
                )
            }
            is Failure -> {
                wooPosLogWrapper.e("Sync $reasonDescription failed: ${syncResult.error}")
            }
        }
    }

    private suspend fun trackSyncSkipped(skipReason: SyncSkipReason) {
        analyticsTracker.track(
            LocalCatalogSyncSkipped(
                syncType = SyncType.INCREMENTAL,
                skipReason = skipReason
            )
        )
    }
}
