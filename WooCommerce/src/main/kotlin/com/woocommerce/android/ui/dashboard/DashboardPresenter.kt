package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.dashboard.DashboardStatsView.StatsTimeframe
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wcStatsStore: WCStatsStore,
    private val selectedSite: SelectedSite
) : DashboardContract.Presenter {
    private var dashboardView: DashboardContract.View? = null

    override fun takeView(view: DashboardContract.View) {
        dashboardView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        dashboardView = null
        dispatcher.unregister(this)
    }

    override fun loadStats(period: StatsTimeframe, forced: Boolean) {
        val granularity = when (period) {
            StatsTimeframe.THIS_WEEK -> StatsGranularity.DAYS
            StatsTimeframe.THIS_MONTH -> StatsGranularity.DAYS
            StatsTimeframe.THIS_YEAR -> StatsGranularity.MONTHS
            StatsTimeframe.YEARS -> StatsGranularity.YEARS
        }

        val payload = FetchOrderStatsPayload(selectedSite.get(), granularity, forced)
        dispatcher.dispatch(WCStatsActionBuilder.newFetchOrderStatsAction(payload))
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

        val revenueStats = wcStatsStore.getRevenueStatsForCurrentMonth(selectedSite.get())
        val orderStats = wcStatsStore.getOrderStatsForCurrentMonth(selectedSite.get())

        dashboardView?.showStats(revenueStats, orderStats, event.granularity)
    }
}
