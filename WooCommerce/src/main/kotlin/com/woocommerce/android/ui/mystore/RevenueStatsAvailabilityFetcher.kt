package com.woocommerce.android.ui.mystore

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsAvailabilityPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RevenueStatsAvailabilityFetcher @Inject constructor(
    private val wcStatsStore: WCStatsStore, // Required to ensure instantiated
    private val dispatcher: Dispatcher
) {
    fun fetchRevenueStatsAvailability(siteModel: SiteModel) {
        val payload = FetchRevenueStatsAvailabilityPayload(siteModel)
        dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAvailabilityAction(payload))
    }
}
