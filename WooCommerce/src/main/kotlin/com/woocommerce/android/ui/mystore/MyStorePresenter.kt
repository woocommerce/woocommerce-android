package com.woocommerce.android.ui.mystore

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.MyStoreContract.Presenter
import com.woocommerce.android.ui.mystore.MyStoreContract.View
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.ui.mystore.domain.GetStats
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MyStorePresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wooCommerceStore: WooCommerceStore, // Required to ensure the WooCommerceStore is initialized!
    private val statsRepository: StatsRepository,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val getStats: GetStats
) : Presenter {
    companion object {
        const val NUM_TOP_PERFORMERS = 3
        const val DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER = 5
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
        showJetpackBenefitsIfNeeded()
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
        }

        coroutineScope.launch {
            getStats(statsRepository, forced, granularity)
                .flowOn(Dispatchers.Default)
                .collect {
                    when (it) {
                        is RevenueStatsSuccess -> myStoreView?.showStats(it.stats, granularity)
                        is GenericError -> myStoreView?.showStatsError(granularity)
                        is HasOrders -> myStoreView?.showEmptyView(!it.hasOrder)
                        is VisitorsStatsError -> myStoreView?.showVisitorStatsError(granularity)
                        is VisitorsStatsSuccess -> myStoreView?.showVisitorStats(it.stats, granularity)
                        PluginNotActive -> myStoreView?.updateStatsAvailabilityError()
                        IsJetPackCPEnabled -> myStoreView?.showEmptyVisitorStatsForJetpackCP()
                        is IsLoading -> myStoreView?.showChartSkeleton(it.isLoading)
                    }
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

        val result = statsRepository.fetchProductLeaderboards(granularity, quantity = NUM_TOP_PERFORMERS, forced)
        handleTopPerformersResult(granularity, result)
    }

    private fun handleTopPerformersResult(
        granularity: StatsGranularity,
        result: Result<List<WCTopPerformerProductModel>>
    ) {
        myStoreView?.showTopPerformersSkeleton(false)
        result.fold(
            onSuccess = { topPerformers ->
                topPerformers
                    .sortedWith(
                        compareByDescending(WCTopPerformerProductModel::quantity)
                            .thenByDescending(WCTopPerformerProductModel::total)
                    ).let {
                        // Track fresh data loaded
                        AnalyticsTracker.track(
                            Stat.DASHBOARD_TOP_PERFORMERS_LOADED,
                            mapOf(AnalyticsTracker.KEY_RANGE to granularity.name.lowercase())
                        )
                        myStoreView?.showTopPerformers(it, granularity)
                    }
            },
            onFailure = {
                myStoreView?.showTopPerformersError(granularity)
            }
        )
    }

    override fun getSelectedSiteName(): String? =
        selectedSite.getIfExists()?.let { site ->
            if (!site.displayName.isNullOrBlank()) {
                site.displayName
            } else {
                site.name
            }
        }

    override fun getStatsCurrency() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

    private fun showJetpackBenefitsIfNeeded() {
        if (selectedSite.getIfExists()?.isJetpackCPConnected == true) {
            val daysSinceDismissal = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - appPrefsWrapper.getJetpackBenefitsDismissalDate()
            )
            myStoreView?.showJetpackBenefitsBanner(daysSinceDismissal >= DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER)
        } else {
            myStoreView?.showJetpackBenefitsBanner(false)
        }
    }

    override fun dismissJetpackBenefitsBanner() {
        myStoreView?.showJetpackBenefitsBanner(false)
        appPrefsWrapper.recordJetpackBenefitsDismissal()
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
