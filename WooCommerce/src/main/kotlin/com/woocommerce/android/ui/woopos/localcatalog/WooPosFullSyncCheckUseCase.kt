package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import javax.inject.Inject

class WooPosFullSyncCheckUseCase @Inject constructor(
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val syncScheduler: WooPosLocalCatalogSyncScheduler,
    private val selectedSite: SelectedSite,
    private val networkStatus: WooPosNetworkStatus,
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
    private val wooPosLogWrapper: WooPosLogWrapper
) {
    suspend fun checkAndTriggerSyncIfNeeded() {
        if (!wooPosLocalCatalogM1Enabled()) {
            wooPosLogWrapper.d("Local catalog feature not enabled")
            return
        }

        if (selectedSite.getOrNull() == null) {
            wooPosLogWrapper.d("No site selected")
            return
        }

        if (!networkStatus.isConnected()) {
            wooPosLogWrapper.d("No network connection")
            return
        }

        if (syncScheduler.isPeriodicWorkRunning() || syncScheduler.isOneTimeWorkRunning()) {
            wooPosLogWrapper.d("Sync already running")
            return
        }

        val lastFullSyncTimestamp = syncTimestampManager.getFullSyncLastCompletedTimestamp()
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000L

        if (lastFullSyncTimestamp == null) {
            wooPosLogWrapper.i("No previous full sync found - triggering immediate sync")
            syncScheduler.triggerManualFullCatalogSync()
            return
        }

        val timeSinceLastSync = currentTime - lastFullSyncTimestamp
        if (timeSinceLastSync > twentyFourHoursInMillis) {
            val hoursSinceSync = timeSinceLastSync / (60 * 60 * 1000)
            wooPosLogWrapper.i("Last full sync was $hoursSinceSync hours ago - triggering immediate sync")
            syncScheduler.triggerManualFullCatalogSync()
        } else {
            val hoursUntilNext = (twentyFourHoursInMillis - timeSinceLastSync) / (60 * 60 * 1000)
            wooPosLogWrapper.d("Full sync is up to date - next sync needed in $hoursUntilNext hours")
        }
    }
}
