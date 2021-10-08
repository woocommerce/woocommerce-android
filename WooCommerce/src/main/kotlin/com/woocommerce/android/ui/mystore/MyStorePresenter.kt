package com.woocommerce.android.ui.mystore

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.MyStoreContract.Presenter
import com.woocommerce.android.ui.mystore.MyStoreContract.View
import com.woocommerce.android.ui.mystore.StatsRepository.StatsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.PLUGIN_NOT_ACTIVE
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class MyStorePresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wooCommerceStore: WooCommerceStore, // Required to ensure the WooCommerceStore is initialized!
    private val wcLeaderboardsStore: WCLeaderboardsStore,
    private val wcStatsStore: WCStatsStore,
    private val statsRepository: StatsRepository,
    @Suppress("UnusedPrivateMember", "Required to ensure the WCOrderStore is initialized!")
    private val wcOrderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus
) : Presenter {
    companion object {
        private val TAG = MyStorePresenter::class.java
        private const val NUM_TOP_PERFORMERS = 3
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
        statsRepository.init()
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)
    }

    override fun dropView() {
        myStoreView = null
        statsRepository.onCleanup()
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

        coroutineScope.launch {
            // check if the store has orders
            val hasNoOrdersTask = async {
                statsRepository.checkIfStoreHasNoOrders()
            }

            // fetch revenue stats
            val revenueStatsTask = async {
                statsRepository.fetchRevenueStats(granularity, forced)
            }

            // fetch visitor stats
            val visitorStatsTask = async {
                statsRepository.fetchVisitorStats(granularity, forced)
            }

            val storeHasNoOrders = hasNoOrdersTask.await().getOrNull()
            if (storeHasNoOrders == true) {
                myStoreView?.showEmptyView(true)
            } else {
                myStoreView?.showEmptyView(false)
                val revenueStatsResult = revenueStatsTask.await()
                val visitorStatsResult = visitorStatsTask.await()
                handleRevenueStatsResult(granularity, revenueStatsResult)
                handleVisitorStatsResults(granularity, visitorStatsResult)
            }
        }
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
                myStoreView?.showTopPerformersSkeleton(true)
            }
        }

        fetchTopPerformersStats(granularity, forceRefresh)
    }

    private fun handleRevenueStatsResult(granularity: StatsGranularity, result: Result<WCRevenueStatsModel?>) {
        myStoreView?.showChartSkeleton(false)
        result.fold(
            onSuccess = { stats ->
                // Track fresh data load
                AnalyticsTracker.track(
                    Stat.DASHBOARD_MAIN_STATS_LOADED,
                    mapOf(AnalyticsTracker.KEY_RANGE to granularity.name.toLowerCase())
                )

                AppPrefs.setV4StatsSupported(true)
                myStoreView?.showStats(stats, granularity)
            },
            onFailure = {
                // display a different error snackbar if the error type is not "plugin not active", since
                // this error is already being handled by the activity class
                if ((it as? StatsException)?.error?.type == PLUGIN_NOT_ACTIVE) {
                    AppPrefs.setV4StatsSupported(false)
                    myStoreView?.updateStatsAvailabilityError()
                } else {
                    myStoreView?.showStatsError(granularity)
                }
            }
        )
    }

    private fun handleVisitorStatsResults(granularity: StatsGranularity, result: Result<Map<String, Int>>) {
        result.fold(
            onSuccess = { visitorStats ->
                myStoreView?.showVisitorStats(visitorStats, granularity)
            },
            onFailure = {
                myStoreView?.showVisitorStatsError(granularity)
            }
        )
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

    override fun getSelectedSiteName(): String? =
        selectedSite.getIfExists()?.let { site ->
            if (!site.displayName.isNullOrBlank()) {
                site.displayName
            } else {
                site.name
            }
        }

    private suspend fun handleTopPerformersResult(
        result: WooResult<List<WCTopPerformerProductModel>>,
        granularity: StatsGranularity
    ) {
        withContext(Dispatchers.Main) {
            myStoreView?.showTopPerformersSkeleton(false)
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
            quantity = NUM_TOP_PERFORMERS
        )

    private fun requestStoredProductLeaderboards(granularity: StatsGranularity) =
        wcLeaderboardsStore.fetchCachedProductLeaderboards(
            site = selectedSite.get(),
            unit = granularity
        )

    override fun getStatsCurrency() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

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
