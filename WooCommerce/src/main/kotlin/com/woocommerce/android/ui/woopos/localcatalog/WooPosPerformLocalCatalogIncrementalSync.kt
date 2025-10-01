package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult.Failure
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult.Success
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Use case for performing incremental sync of the local catalog in background.
 *
 * This class encapsulates the common logic for triggering incremental sync that was duplicated
 * across multiple ViewModels (WooPosTotalsViewModel and WooPosSplashViewModel).
 *
 * The sync:
 * - Only runs when WOO_POS_LOCAL_CATALOG_M1 feature flag is enabled
 * - Only runs when network is connected
 * - Runs in application scope to survive ViewModel lifecycle
 * - Logs comprehensive information about sync results
 */
class WooPosPerformLocalCatalogIncrementalSync @Inject constructor(
    private val localCatalogSyncRepository: WooPosLocalCatalogSyncRepository,
    private val selectedSite: SelectedSite,
    private val networkStatus: WooPosNetworkStatus,
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
    private val wooPosLogWrapper: WooPosLogWrapper,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    /**
     * Executes incremental sync in the background.
     *
     * @param reason A descriptive reason for the sync (e.g., "after successful payment", "on splash screen")
     *               Used for logging to help with debugging and monitoring.
     */
    fun execute(reason: String) {
        if (!wooPosLocalCatalogM1Enabled()) {
            wooPosLogWrapper.d("Skipping sync $reason: Local catalog feature not enabled")
            return
        }

        if (!networkStatus.isConnected()) {
            wooPosLogWrapper.d("Skipping sync $reason: No network connection")
            return
        }

        appCoroutineScope.launch {
            selectedSite.getOrNull()?.let { site ->
                wooPosLogWrapper.d("Starting incremental sync $reason")
                val syncResult = localCatalogSyncRepository.syncLocalCatalogIncremental(site)
                when (syncResult) {
                    is Success -> {
                        wooPosLogWrapper.d(
                            "Sync $reason completed successfully: " +
                                "${syncResult.productsSynced} products, " +
                                "${syncResult.variationsSynced} variations synced " +
                                "in ${syncResult.syncDurationMs}ms"
                        )
                    }
                    is Failure -> {
                        wooPosLogWrapper.e("Sync $reason failed: ${syncResult.error}")
                    }
                }
            } ?: wooPosLogWrapper.d("Skipping sync $reason: No site selected")
        }
    }
}
