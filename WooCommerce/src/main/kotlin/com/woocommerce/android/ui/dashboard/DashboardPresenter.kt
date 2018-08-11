package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.DASHBOARD
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCTopEarnersChanged
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wcStatsStore: WCStatsStore,
    private val selectedSite: SelectedSite
) : DashboardContract.Presenter {
    private var dashboardView: DashboardContract.View? = null
    companion object {
        private const val NUM_TOP_EARNERS = 3
        private const val FORCE_REQUEST_FREQUENCY = (1000 * 60) * 5 // force request every 5 minutes
    }

    private val topEarnersLastForceTime = LongArray(StatsGranularity.values().size)

    override fun takeView(view: DashboardContract.View) {
        dashboardView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        dashboardView = null
        dispatcher.unregister(this)
    }

    override fun loadStats(granularity: StatsGranularity, forced: Boolean) {
        val payload = FetchOrderStatsPayload(selectedSite.get(), granularity, forced)
        dispatcher.dispatch(WCStatsActionBuilder.newFetchOrderStatsAction(payload))
    }

    override fun loadTopEarnerStats(granularity: StatsGranularity, forced: Boolean) {
        val shouldForce = if (forced) {
            true
        } else {
            // is it time to force an update for this granularity?
            val lastForced = topEarnersLastForceTime[granularity.ordinal]
            val diff = System.currentTimeMillis() - lastForced
            lastForced == 0L || diff >= FORCE_REQUEST_FREQUENCY
        }
        if (shouldForce) {
            topEarnersLastForceTime[granularity.ordinal] = System.currentTimeMillis()
        }
        WooLog.d(DASHBOARD, "Forcing top earners $granularity = $shouldForce")

        dashboardView?.clearTopEarners()
        val payload = FetchTopEarnersStatsPayload(selectedSite.get(), granularity, NUM_TOP_EARNERS, shouldForce)
        dispatcher.dispatch(WCStatsActionBuilder.newFetchTopEarnersStatsAction(payload))
    }

    override fun getStatsCurrency() = wcStatsStore.getStatsCurrencyForSite(selectedSite.get())

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCStatsChanged(event: OnWCStatsChanged) {
        if (event.isError) {
            // TODO: Notify the user of the problem
            // For now, send empty data so views aren't stuck in loading mode
            dashboardView?.showStats(emptyMap(), emptyMap(), event.granularity)
            return
        }

        val revenueStats = wcStatsStore.getRevenueStats(selectedSite.get(), event.granularity)
        val orderStats = wcStatsStore.getOrderStats(selectedSite.get(), event.granularity)

        dashboardView?.showStats(revenueStats, orderStats, event.granularity)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCTopEarnersChanged(event: OnWCTopEarnersChanged) {
        if (event.isError) {
            // TODO: notify user of the problem?
            dashboardView?.hideTopEarners()
        } else {
            dashboardView?.showTopEarners(event.topEarners, event.granularity)
        }
    }
}
