package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
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
    companion object {
        private val TAG = DashboardPresenter::class.java
    }

    private var dashboardView: DashboardContract.View? = null

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

    override fun getStatsCurrency() = wcStatsStore.getStatsCurrencyForSite(selectedSite.get())

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCStatsChanged(event: OnWCStatsChanged) {
        if (event.isError) {
            WooLog.e(T.DASHBOARD, "$TAG - Error fetching stats: ${event.error.message}")
            // TODO: Notify the user of the problem
            // For now, send empty data so views aren't stuck in loading mode
            dashboardView?.showStats(emptyMap(), emptyMap(), event.granularity)
            return
        }

        val revenueStats = wcStatsStore.getRevenueStats(selectedSite.get(), event.granularity)
        val orderStats = wcStatsStore.getOrderStats(selectedSite.get(), event.granularity)

        dashboardView?.showStats(revenueStats, orderStats, event.granularity)
    }
}
