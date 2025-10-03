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

class WooPosPerformLocalCatalogIncrementalSync @Inject constructor(
    private val localCatalogSyncRepository: WooPosLocalCatalogSyncRepository,
    private val selectedSite: SelectedSite,
    private val networkStatus: WooPosNetworkStatus,
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
    private val wooPosLogWrapper: WooPosLogWrapper,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    fun execute(reason: WooPosIncrementalSyncReason) {
        val reasonDescription = reason.description

        if (!wooPosLocalCatalogM1Enabled()) {
            wooPosLogWrapper.d("Skipping sync $reasonDescription: Local catalog feature not enabled")
            return
        }

        if (!networkStatus.isConnected()) {
            wooPosLogWrapper.d("Skipping sync $reasonDescription: No network connection")
            return
        }

        appCoroutineScope.launch {
            selectedSite.getOrNull()?.let { site ->
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
            } ?: wooPosLogWrapper.d("Skipping sync $reasonDescription: No site selected")
        }
    }
}
