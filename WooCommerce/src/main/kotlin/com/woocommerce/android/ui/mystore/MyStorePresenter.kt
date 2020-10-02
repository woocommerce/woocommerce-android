package com.woocommerce.android.ui.mystore

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.MyStoreContract.Presenter
import com.woocommerce.android.ui.mystore.MyStoreContract.View
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_HAS_ORDERS
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_NEW_VISITOR_STATS
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_REVENUE_STATS
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchNewVisitorStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCRevenueStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.PLUGIN_NOT_ACTIVE
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@ActivityScope
class MyStorePresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wooCommerceStore: WooCommerceStore, // Required to ensure the WooCommerceStore is initialized!
    private val wcLeaderboardsStore: WCLeaderboardsStore,
    private val wcStatsStore: WCStatsStore,
    private val wcOrderStore: WCOrderStore, // Required to ensure the WCOrderStore is initialized!
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus
) : Presenter {
    companion object {
        private val TAG = MyStorePresenter::class.java
        private const val NUM_TOP_EARNERS = 3
        private val statsForceRefresh = BooleanArray(StatsGranularity.values().size)
        private val topPerformersForceRefresh = BooleanArray(StatsGranularity.values().size)

        init {
            resetForceRefresh()
        }

        /**
         * this tells the presenter to force a refresh for all granularities on the next request - this is
         * used after a swipe-to-refresh on the dashboard to ensure we don't get cached data
         */
        fun resetForceRefresh() {
            for (i in statsForceRefresh.indices) {
                statsForceRefresh[i] = true
            }
            for (i in topPerformersForceRefresh.indices) {
                topPerformersForceRefresh[i] = true
            }
        }
    }

    private var myStoreView: View? = null

    override fun takeView(view: View) {
        myStoreView = view
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)
    }

    override fun dropView() {
        myStoreView = null
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
    }

    override fun loadStats(granularity: StatsGranularity, forced: Boolean) {
        if (!networkStatus.isConnected()) {
            myStoreView?.isRefreshPending = true
            return
        }

        val forceRefresh = forced || statsForceRefresh[granularity.ordinal]
        if (forceRefresh) {
            statsForceRefresh[granularity.ordinal] = false
            myStoreView?.showChartSkeleton(true)
        }

        // fetch revenue stats
        fetchRevenueStats(granularity, forceRefresh)

        // fetch visitor stats
        fetchVisitorStats(granularity, forceRefresh)
    }

    override suspend fun loadTopPerformersStats(
        granularity: StatsGranularity,
        forced: Boolean
    ) {
        if (!networkStatus.isConnected()) {
            myStoreView?.isRefreshPending = true
        }

        val forceRefresh = forced || topPerformersForceRefresh[granularity.ordinal]
        if (forceRefresh) {
            topPerformersForceRefresh[granularity.ordinal] = false
            withContext(Dispatchers.Main) {
                myStoreView?.showTopEarnersSkeleton(true)
            }
        }

        fetchTopPerformersStats(granularity, forceRefresh)
    }

    override fun fetchRevenueStats(granularity: StatsGranularity, forced: Boolean) {
        val statsPayload = FetchRevenueStatsPayload(selectedSite.get(), granularity, forced = forced)
        dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAction(statsPayload))
    }

    override fun fetchVisitorStats(granularity: StatsGranularity, forced: Boolean) {
        val visitsPayload = FetchNewVisitorStatsPayload(selectedSite.get(), granularity, forced)
        dispatcher.dispatch(WCStatsActionBuilder.newFetchNewVisitorStatsAction(visitsPayload))
    }

    override suspend fun fetchTopPerformersStats(
        granularity: StatsGranularity,
        forced: Boolean
    ) {
        withContext(Dispatchers.Default) {
            requestProductLeaderboards(granularity, forced)
                .also { handleTopPerformersResult(it, granularity) }
        }
    }

    private suspend fun handleTopPerformersResult(
        result: WooResult<List<WCTopPerformerProductModel>>,
        granularity: StatsGranularity
    ) {
        withContext(Dispatchers.Main) {
            myStoreView?.showTopEarnersSkeleton(false)
            result.model?.let {
                onWCTopPerformersChanged(it, granularity)
            } ?: myStoreView?.showTopPerformersError(granularity)
        }
    }

    private suspend fun requestProductLeaderboards(granularity: StatsGranularity, forced: Boolean) =
        when (forced) {
            true -> requestUpdatedProductLeaderboards(granularity)
            false -> requestStoredProductLeaderboards(granularity)
        }

    private suspend fun requestUpdatedProductLeaderboards(granularity: StatsGranularity) =
        wcLeaderboardsStore.fetchProductLeaderboards(
            site = selectedSite.get(),
            unit = granularity,
            quantity = NUM_TOP_EARNERS
        )

    private fun requestStoredProductLeaderboards(granularity: StatsGranularity) =
        wcLeaderboardsStore.fetchCachedProductLeaderboards(
            site = selectedSite.get(),
            unit = granularity
        )

    override fun getStatsCurrency() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

    /**
     * dispatches a FETCH_HAS_ORDERS action which tells us whether this store has *ever* had any orders
     */
    override fun fetchHasOrders() {
        val payload = FetchHasOrdersPayload(selectedSite.get())
        dispatcher.dispatch(WCOrderActionBuilder.newFetchHasOrdersAction(payload))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCRevenueStatsChanged(event: OnWCRevenueStatsChanged) {
        when (event.causeOfChange) {
            FETCH_REVENUE_STATS -> {
                myStoreView?.showChartSkeleton(false)
                if (event.isError) {
                    WooLog.e(T.DASHBOARD, "$TAG - Error fetching stats: ${event.error.message}")
                    // display a different error snackbar if the error type is not "plugin not active", since
                    // this error is already being handled by the activity class
                    if (event.error.type == PLUGIN_NOT_ACTIVE) {
                        AppPrefs.setV4StatsSupported(false)
                        myStoreView?.updateStatsAvailabilityError()
                    } else {
                        myStoreView?.showStatsError(event.granularity)
                    }
                    return
                }

                // Track fresh data load
                AnalyticsTracker.track(
                    Stat.DASHBOARD_MAIN_STATS_LOADED,
                    mapOf(AnalyticsTracker.KEY_RANGE to event.granularity.name.toLowerCase())
                )

                val revenueStatsModel = wcStatsStore.getRawRevenueStats(
                    selectedSite.get(), event.granularity, event.startDate!!, event.endDate!!
                )
                myStoreView?.showStats(revenueStatsModel, event.granularity)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCStatsChanged(event: OnWCStatsChanged) {
        when (event.causeOfChange) {
            FETCH_NEW_VISITOR_STATS -> {
                if (event.isError) {
                    WooLog.e(T.DASHBOARD, "$TAG - Error fetching visitor stats: ${event.error.message}")
                    myStoreView?.showVisitorStatsError(event.granularity)
                    return
                }

                val visitorStats = wcStatsStore.getNewVisitorStats(
                    selectedSite.get(), event.granularity, event.quantity, event.date, event.isCustomField
                )
                myStoreView?.showVisitorStats(visitorStats, event.granularity)
            }
        }
    }

    fun onWCTopPerformersChanged(
        topPerformers: List<WCTopPerformerProductModel>?,
        granularity: StatsGranularity
    ) {
        topPerformers
            ?.sortedWith(
                compareByDescending(WCTopPerformerProductModel::quantity)
                    .thenByDescending(WCTopPerformerProductModel::total)
            )?.let {
                // Track fresh data loaded
                AnalyticsTracker.track(
                    Stat.DASHBOARD_TOP_PERFORMERS_LOADED,
                    mapOf(AnalyticsTracker.KEY_RANGE to granularity.name.toLowerCase())
                )
                myStoreView?.showTopPerformers(it, granularity)
            } ?: myStoreView?.showTopPerformersError(granularity)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        when (event.causeOfChange) {
            FETCH_HAS_ORDERS -> {
                if (event.isError) {
                    WooLog.e(
                        T.DASHBOARD,
                        "$TAG - Error fetching whether orders exist: ${event.error.message}"
                    )
                } else {
                    val hasNoOrders = event.rowsAffected == 0
                    myStoreView?.showEmptyView(hasNoOrders)
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh data if needed now that a connection is active
            myStoreView?.let { view ->
                if (view.isRefreshPending) {
                    view.refreshMyStoreStats(forced = false)
                }
            }
        }
    }
}
