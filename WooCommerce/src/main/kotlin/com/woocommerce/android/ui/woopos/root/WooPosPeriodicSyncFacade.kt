package com.woocommerce.android.ui.woopos.root

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIncrementalSyncReason
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIsLocalCatalogSupported
import com.woocommerce.android.ui.woopos.localcatalog.WooPosPerformLocalCatalogIncrementalSync
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

/**
 * Lifecycle-aware facade that performs periodic incremental syncs while [WooPosActivity] is visible.
 * The sync runs once per hour while the POS activity is in the foreground (between onResume and onPause).
 */
@Singleton
class WooPosPeriodicSyncFacade @Inject constructor(
    private val incrementalSync: WooPosPerformLocalCatalogIncrementalSync,
    private val selectedSite: SelectedSite,
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported,
) : DefaultLifecycleObserver {

    private var periodicSyncJob: Job? = null

    private companion object {
        val SYNC_INTERVAL = 1.hours.inWholeMilliseconds
    }

    override fun onResume(owner: LifecycleOwner) {
        startPeriodicSync(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        stopPeriodicSync()
    }

    private fun startPeriodicSync(owner: LifecycleOwner) {
        periodicSyncJob = owner.lifecycleScope.launch {
            val site = selectedSite.getOrNull()
                ?: error("No site selected")
            if (!isLocalCatalogSupported(site.siteId)) return@launch

            while (isActive) {
                incrementalSync.execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)
                delay(SYNC_INTERVAL)
            }
        }
    }

    private fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
    }
}
