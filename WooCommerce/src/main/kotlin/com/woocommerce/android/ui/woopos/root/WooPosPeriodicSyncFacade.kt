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
import kotlin.time.Duration.Companion.minutes

/**
 * Lifecycle-aware facade that performs periodic incremental syncs while [WooPosActivity] is visible.
 * The sync runs once per hour while the POS activity is in the foreground (between onResume and onPause).
 */
@Singleton
class WooPosPeriodicSyncFacade @Inject constructor(
    private val incrementalSync: WooPosPerformLocalCatalogIncrementalSync,
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported,
    private val selectedSite: SelectedSite,
) : DefaultLifecycleObserver {

    private var periodicSyncJob: Job? = null

    private companion object {
        val SYNC_INTERVAL = 1.minutes.inWholeMilliseconds
    }

    override fun onResume(owner: LifecycleOwner) {
        startPeriodicSync(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        stopPeriodicSync()
    }

    private fun startPeriodicSync(owner: LifecycleOwner) {
        val site = selectedSite.getOrNull()
            ?: error("No site selected")

        periodicSyncJob = owner.lifecycleScope.launch {
            if (isLocalCatalogSupported(site.siteId)) {
                while (isActive) {
                    incrementalSync.execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)
                    delay(SYNC_INTERVAL)
                }
            }
        }
    }

    private fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
    }
}
