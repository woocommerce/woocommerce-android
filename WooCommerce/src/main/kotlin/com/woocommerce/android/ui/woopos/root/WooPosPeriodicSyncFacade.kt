package com.woocommerce.android.ui.woopos.root

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.ui.woopos.localcatalog.DateTimeProvider
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIncrementalSyncReason
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIsLocalCatalogSupported
import com.woocommerce.android.ui.woopos.localcatalog.WooPosPerformLocalCatalogIncrementalSync
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Lifecycle-aware facade that performs periodic incremental syncs while [WooPosActivity] is visible.
 * The sync runs at most once per hour while the POS activity is in the foreground (between onResume and onPause).
 * It checks the last incremental sync timestamp to avoid redundant syncs when other events
 * (e.g., successful payment, POS home) have already triggered recent syncs.
 */
@Singleton
class WooPosPeriodicSyncFacade @Inject constructor(
    private val incrementalSync: WooPosPerformLocalCatalogIncrementalSync,
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported,
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val time: DateTimeProvider,
) : DefaultLifecycleObserver {

    private var periodicSyncJob: Job? = null

    private companion object {
        val INCREMENTAL_SYNC_INTERVAL = 1.hours.inWholeMilliseconds
        val SYNC_STATUS_CHECK_INTERVAL = 10.minutes.inWholeMilliseconds
    }

    override fun onResume(owner: LifecycleOwner) {
        startPeriodicSync(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        stopPeriodicSync()
    }

    private fun startPeriodicSync(owner: LifecycleOwner) {
        periodicSyncJob = owner.lifecycleScope.launch {
            if (!isLocalCatalogSupported()) return@launch

            while (isActive) {
                delay(SYNC_STATUS_CHECK_INTERVAL)

                if (isLocalCatalogRequiresIncrementalSync()) {
                    incrementalSync.execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)
                }
            }
        }
    }

    private suspend fun isLocalCatalogRequiresIncrementalSync(): Boolean {
        val productsTimestamp = syncTimestampManager.getProductsLastSyncTimestamp()
        val variationsTimestamp = syncTimestampManager.getVariationsLastSyncTimestamp()
        val currentTime = time.now()

        return if (productsTimestamp != null && variationsTimestamp != null) {
            val timeSinceProductsSync = currentTime - productsTimestamp
            val timeSinceVariationsSync = currentTime - variationsTimestamp
            timeSinceProductsSync >= INCREMENTAL_SYNC_INTERVAL || timeSinceVariationsSync >= INCREMENTAL_SYNC_INTERVAL
        } else {
            true
        }
    }

    private fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
    }
}
