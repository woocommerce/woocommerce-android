package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus.PROCESSING
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
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
    private val wcOrderStore: WCOrderStore, // Required to ensure the WCOrderStore is initialized!
    private val selectedSite: SelectedSite
) : DashboardContract.Presenter {
    companion object {
        private val TAG = DashboardPresenter::class.java
        private const val NUM_TOP_EARNERS = 3
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

    override fun loadTopEarnerStats(granularity: StatsGranularity, forced: Boolean) {
        val payload = FetchTopEarnersStatsPayload(selectedSite.get(), granularity, NUM_TOP_EARNERS, forced)
        dispatcher.dispatch(WCStatsActionBuilder.newFetchTopEarnersStatsAction(payload))
    }

    override fun getStatsCurrency() = wcStatsStore.getStatsCurrencyForSite(selectedSite.get())

    override fun fetchUnfilledOrderCount() {
        val payload = FetchOrdersCountPayload(selectedSite.get(), PROCESSING.value)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersCountAction(payload))
    }

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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCTopEarnersChanged(event: OnWCTopEarnersChanged) {
        if (event.isError) {
            dashboardView?.showTopEarnersError(event.granularity)
        } else {
            dashboardView?.showTopEarners(event.topEarners, event.granularity)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        event.causeOfChange?.takeIf { it == WCOrderAction.FETCH_ORDERS_COUNT }?.let { _ ->
            if (event.isError) {
                WooLog.e(T.DASHBOARD,
                        "$TAG - Error fetching a count of orders waiting to be fulfilled: ${event.error.message}")
                dashboardView?.hideUnfilledOrdersCard()
                return
            }
            event.rowsAffected.takeIf { it > 0 }?.let { count ->
                dashboardView?.showUnfilledOrdersCard(count, event.canLoadMore)
            } ?: dashboardView?.hideUnfilledOrdersCard()
        } ?: if (!event.isError && !isIgnoredOrderEvent(event.causeOfChange)) {
            dashboardView?.refreshDashboard()
        }
    }

    /**
     * Use this function to add [OnOrderChanged] events that should be ignored.
     */
    private fun isIgnoredOrderEvent(actionType: WCOrderAction?): Boolean {
        return actionType == null ||
                actionType == WCOrderAction.FETCH_ORDER_NOTES ||
                actionType == WCOrderAction.POST_ORDER_NOTE
    }
}
