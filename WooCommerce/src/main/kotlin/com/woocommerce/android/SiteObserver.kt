package com.woocommerce.android

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.environment.EnvironmentRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.wear.WearableConnectionRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

/**
 * A utility class that can be used to force fetching data specific to current site,
 * the fetching will occur on app launch, and on each site switching
 *
 * TODO: check and move other relevant pieces to this class, currently it's used only for fetching plugins
 */
@Suppress("ForbiddenComment")
class SiteObserver @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val environmentRepository: EnvironmentRepository,
    private val connRepository: WearableConnectionRepository
) {
    suspend fun observeAndUpdateSelectedSiteData() {
        selectedSite.observe()
            .filterNotNull()
            .distinctUntilChanged { old, new -> new.id == old.id }
            .collect { site ->
                WooLog.d(WooLog.T.UTILS, "Fetch plugins for site ${site.name}")
                wooCommerceStore.fetchSitePlugins(site)

                // Makes sure the store ID is fetched for the site.
                environmentRepository.fetchOrGetStoreID(site)
                    .takeIf { result -> result.isError.not() }
                    ?.model?.let { storeID ->
                        WooLog.d(WooLog.T.UTILS, "Fetched StoreID $storeID for site ${site.name}")
                    }

                connRepository.sendTokenData()
            }
    }
}
