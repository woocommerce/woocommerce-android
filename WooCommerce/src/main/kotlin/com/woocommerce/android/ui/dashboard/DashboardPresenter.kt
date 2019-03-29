package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_HAS_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS_COUNT
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_ORDER_STATS
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_VISITOR_STATS
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus.PROCESSING
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchVisitorStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCTopEarnersChanged
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wooCommerceStore: WooCommerceStore, // Required to ensure the WooCommerceStore is initialized!
    private val wcStatsStore: WCStatsStore,
    private val wcOrderStore: WCOrderStore, // Required to ensure the WCOrderStore is initialized!
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus
) : DashboardContract.Presenter {
    companion object {
        private val TAG = DashboardPresenter::class.java
        private const val NUM_TOP_EARNERS = 3
        private val statsForceRefresh = BooleanArray(StatsGranularity.values().size)
        private val topEarnersForceRefresh = BooleanArray(StatsGranularity.values().size)

        init {
            resetForceRefresh()
        }

        /**
         * this tells the presenter to force a refresh for all granularities on the next request - this is
         * used after a swipe-to-refresh on the dashboard to ensure we don't get cached data
         */
        fun resetForceRefresh() {
            for (i in 0 until statsForceRefresh.size) {
                statsForceRefresh[i] = true
            }
            for (i in 0 until topEarnersForceRefresh.size) {
                topEarnersForceRefresh[i] = true
            }
        }
    }

    private var dashboardView: DashboardContract.View? = null

    override fun takeView(view: DashboardContract.View) {
        dashboardView = view
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)
    }

    override fun dropView() {
        dashboardView = null
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
    }

    override fun loadStats(granularity: StatsGranularity, forced: Boolean) {
        if (!networkStatus.isConnected()) {
            dashboardView?.isRefreshPending = true
            return
        }

        val forceRefresh = forced || statsForceRefresh[granularity.ordinal]
        if (forceRefresh) {
            statsForceRefresh[granularity.ordinal] = false
            dashboardView?.showChartSkeleton(true)
        }
        val statsPayload = FetchOrderStatsPayload(selectedSite.get(), granularity, forced = forceRefresh)
        dispatcher.dispatch(WCStatsActionBuilder.newFetchOrderStatsAction(statsPayload))

        // fetch visitor stats
        val visitsPayload = FetchVisitorStatsPayload(selectedSite.get(), granularity, forced = forceRefresh)
        dispatcher.dispatch(WCStatsActionBuilder.newFetchVisitorStatsAction(visitsPayload))
    }

    override fun loadTopEarnerStats(granularity: StatsGranularity, forced: Boolean) {
        if (!networkStatus.isConnected()) {
            dashboardView?.isRefreshPending = true
            return
        }

        val forceRefresh = forced || topEarnersForceRefresh[granularity.ordinal]
        if (forceRefresh) {
            topEarnersForceRefresh[granularity.ordinal] = false
            dashboardView?.showTopEarnersSkeleton(true)
        }

        val payload = FetchTopEarnersStatsPayload(
                selectedSite.get(), granularity, NUM_TOP_EARNERS, forced = forceRefresh)
        dispatcher.dispatch(WCStatsActionBuilder.newFetchTopEarnersStatsAction(payload))
    }

    override fun getStatsCurrency() = wcStatsStore.getStatsCurrencyForSite(selectedSite.get())

    override fun fetchUnfilledOrderCount(forced: Boolean) {
        if (!networkStatus.isConnected()) {
            dashboardView?.isRefreshPending = true
            return
        }

        if (forced) {
            dashboardView?.showUnfilledOrdersSkeleton(true)
        }
        val payload = FetchOrdersCountPayload(selectedSite.get(), PROCESSING.value)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersCountAction(payload))
    }

    /**
     * dispatches a FETCH_HAS_ORDERS action which tells us whether this store has *ever* had any orders
     */
    override fun fetchHasOrders() {
        val payload = FetchHasOrdersPayload(selectedSite.get())
        dispatcher.dispatch(WCOrderActionBuilder.newFetchHasOrdersAction(payload))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCStatsChanged(event: OnWCStatsChanged) {
        when (event.causeOfChange) {
            FETCH_ORDER_STATS -> {
                dashboardView?.showChartSkeleton(false)
                if (event.isError) {
                    WooLog.e(T.DASHBOARD, "$TAG - Error fetching stats: ${event.error.message}")
                    dashboardView?.showStatsError(event.granularity)
                    return
                }

                // Track fresh data load
                AnalyticsTracker.track(
                        Stat.DASHBOARD_MAIN_STATS_LOADED,
                        mapOf(AnalyticsTracker.KEY_RANGE to event.granularity.name.toLowerCase()))

                val revenueStats = wcStatsStore.getRevenueStats(selectedSite.get(), event.granularity)
                val orderStats = wcStatsStore.getOrderStats(selectedSite.get(), event.granularity)

                dashboardView?.showStats(revenueStats, orderStats, event.granularity)
            }

            FETCH_VISITOR_STATS -> {
                if (event.isError) {
                    WooLog.e(T.DASHBOARD, "$TAG - Error fetching visitor stats: ${event.error.message}")
                    dashboardView?.showVisitorStatsError(event.granularity)
                    return
                }

                val visits = event.rowsAffected
                dashboardView?.showVisitorStats(visits, event.granularity)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCTopEarnersChanged(event: OnWCTopEarnersChanged) {
        dashboardView?.showTopEarnersSkeleton(false)
        if (event.isError) {
            dashboardView?.showTopEarnersError(event.granularity)
        } else {
            // Track fresh data loaded
            AnalyticsTracker.track(
                    Stat.DASHBOARD_TOP_PERFORMERS_LOADED,
                    mapOf(AnalyticsTracker.KEY_RANGE to event.granularity.name.toLowerCase()))

            dashboardView?.showTopEarners(event.topEarners, event.granularity)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        when (event.causeOfChange) {
            FETCH_HAS_ORDERS -> {
                if (event.isError) {
                    WooLog.e(T.DASHBOARD,
                            "$TAG - Error fetching whether orders exist: ${event.error.message}")
                } else {
                    val hasNoOrders = event.rowsAffected == 0
                    dashboardView?.showEmptyView(hasNoOrders)
                }
            }
            FETCH_ORDERS_COUNT -> {
                dashboardView?.showUnfilledOrdersSkeleton(false)
                if (event.isError) {
                    WooLog.e(T.DASHBOARD,
                            "$TAG - Error fetching a count of orders waiting to be fulfilled: ${event.error.message}")
                    dashboardView?.hideUnfilledOrdersCard()
                    return
                }

                // Track fresh data loaded
                AnalyticsTracker.track(
                        Stat.DASHBOARD_UNFULFILLED_ORDERS_LOADED,
                        mapOf(AnalyticsTracker.KEY_HAS_UNFULFILLED_ORDERS to (event.rowsAffected > 0)))

                event.rowsAffected.takeIf { it > 0 }?.let { count ->
                    dashboardView?.showUnfilledOrdersCard(count)
                } ?: dashboardView?.hideUnfilledOrdersCard()
            }
            else -> {
                if (!event.isError && !isIgnoredOrderEvent(event.causeOfChange)) {
                    dashboardView?.refreshDashboard(forced = false)
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh data if needed now that a connection is active
            dashboardView?.let { view ->
                if (view.isRefreshPending) {
                    view.refreshDashboard(forced = false)
                }
            }
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
