package com.woocommerce.android

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.environment.EnvironmentRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.UTILS
import com.woocommerce.android.util.dispatchAndAwait
import com.woocommerce.android.wear.WearableConnectionRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
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
    private val wearableConnectionRepository: WearableConnectionRepository,
    private val dispatcher: Dispatcher
) {
    suspend fun observeAndUpdateSelectedSiteData() {
        selectedSite.observe()
            .filterNotNull()
            .distinctUntilChanged { old, new -> new.id == old.id }
            .collectLatest { site ->
                coroutineScope {
                    launch { fetchPlugins(site) }

                    launch { fetchStoreId(site) }

                    launch { fetchOrderStatusOptions(site) }

                    launch { sendSiteDataToWearable(site) }
                }
            }
    }

    private suspend fun fetchPlugins(site: SiteModel) {
        WooLog.d(WooLog.T.UTILS, "Fetch plugins for site ${site.name}")
        wooCommerceStore.fetchSitePlugins(site)
    }

    private suspend fun fetchStoreId(site: SiteModel) {
        // Makes sure the store ID is fetched for the site.
        environmentRepository.fetchOrGetStoreID(site)
            .takeIf { result -> result.isError.not() }
            ?.model?.let { storeID ->
                WooLog.d(UTILS, "Fetched StoreID $storeID for site ${site.name}")
            }
    }

    private suspend fun fetchOrderStatusOptions(site: SiteModel) {
        WooLog.d(WooLog.T.UTILS, "Fetch status options for site ${site.name}")
        dispatcher.dispatchAndAwait<FetchOrderStatusOptionsPayload, OnOrderStatusOptionsChanged>(
            WCOrderActionBuilder.newFetchOrderStatusOptionsAction(
                FetchOrderStatusOptionsPayload(site)
            )
        )
    }

    private fun sendSiteDataToWearable(site: SiteModel) {
        WooLog.d(WooLog.T.UTILS, "Sending site ${site.name} to connected Wearables")
        wearableConnectionRepository.sendSiteData(site)
    }
}
