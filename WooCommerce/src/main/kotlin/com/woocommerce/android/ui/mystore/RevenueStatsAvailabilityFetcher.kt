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
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
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
        // The event.availability flag would be false,
        // if there an error when fetching stats:
        // When no internet: We could display
        if (event.causeOfChange == FETCH_REVENUE_STATS_AVAILABILITY) {
            // update the v4 stats availability to SharedPreferences
            // only if there is no error OR if the error is because of plugin not available
            // this is because we don't want to update the availability if the error response is due of network issues
            if (!event.isError || (event.isError && event.error?.type == OrderStatsErrorType.PLUGIN_NOT_ACTIVE)) {
                AppPrefs.setV4StatsSupported(event.availability)
                EventBus.getDefault().post(RevenueStatsAvailabilityChangeEvent(event.availability))
            }
        }
    }
}
