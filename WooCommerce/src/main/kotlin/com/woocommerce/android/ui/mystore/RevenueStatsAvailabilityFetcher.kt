package com.woocommerce.android.ui.mystore

import com.woocommerce.android.AppPrefs
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_REVENUE_STATS_AVAILABILITY
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsAvailabilityPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCRevenueStatsChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RevenueStatsAvailabilityFetcher @Inject constructor(
    private val wcStatsStore: WCStatsStore, // Required to ensure instantiated
    private val dispatcher: Dispatcher
) {
    init {
        dispatcher.register(this)
    }

    fun fetchRevenueStatsAvailability(siteModel: SiteModel) {
        val payload = FetchRevenueStatsAvailabilityPayload(siteModel)
        dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAvailabilityAction(payload))
    }

    class RevenueStatsAvailabilityChangeEvent(var available: Boolean)

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCRevenueStatsChanged(event: OnWCRevenueStatsChanged) {
        if (event.causeOfChange == FETCH_REVENUE_STATS_AVAILABILITY) {
            // update the v4 stats availability to SharedPreferences
            AppPrefs.setIsUsingV4Api(event.availability)
            EventBus.getDefault().post(RevenueStatsAvailabilityChangeEvent(event.availability))
        }
    }
}
