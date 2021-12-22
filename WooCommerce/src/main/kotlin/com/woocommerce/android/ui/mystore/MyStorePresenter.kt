package com.woocommerce.android.ui.mystore

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.MyStoreContract.Presenter
import com.woocommerce.android.ui.mystore.MyStoreContract.View
import com.woocommerce.android.ui.mystore.domain.GetStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ExperimentalCoroutinesApi
class MyStorePresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wooCommerceStore: WooCommerceStore, // Required to ensure the WooCommerceStore is initialized!
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val getStats: GetStats
) : Presenter {
    companion object {
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
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)
        showJetpackBenefitsIfNeeded()
    }

    override fun dropView() {
        myStoreView = null
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
        super.dropView()
    }

//    override fun loadStats(granularity: StatsGranularity, forced: Boolean) {
//        if (!networkStatus.isConnected()) {
//            myStoreView?.isRefreshPending = true
//            return
//        }
//
//        val forceRefresh = forced || statsForceRefresh[granularity.ordinal]
//        if (forceRefresh) {
//            statsForceRefresh[granularity.ordinal] = false
//        }
//        myStoreView?.showChartSkeleton(true)
//        coroutineScope.launch {
//            getStats(forced, granularity)
//                .collect {
//                    when (it) {
//                        is RevenueStatsSuccess -> {
//                            myStoreView?.showStats(it.stats, granularity)
//                            AnalyticsTracker.track(
//                                AnalyticsTracker.Stat.DASHBOARD_MAIN_STATS_LOADED,
//                                mapOf(AnalyticsTracker.KEY_RANGE to granularity.name.lowercase())
//                            )
//                        }
//                        is RevenueStatsError -> myStoreView?.showStatsError(granularity)
//                        is HasOrders -> myStoreView?.showEmptyView(!it.hasOrder)
//                        is VisitorsStatsError -> myStoreView?.showVisitorStatsError(granularity)
//                        is VisitorsStatsSuccess -> myStoreView?.showVisitorStats(it.stats, granularity)
//                        PluginNotActive -> myStoreView?.updateStatsAvailabilityError()
//                        IsJetPackCPEnabled -> myStoreView?.showEmptyVisitorStatsForJetpackCP()
//                    }
//                    if (it is RevenueStatsSuccess || it is PluginNotActive || it is RevenueStatsError) {
//                        myStoreView?.showChartSkeleton(false)
//                    }
//                }
//        }
//    }

//    override fun loadTopPerformersStats(
//        granularity: StatsGranularity,
//        forced: Boolean
//    ) {
//        if (!networkStatus.isConnected()) {
//            myStoreView?.isRefreshPending = true
//        }
//
//        val forceRefresh = forced || topPerformersForceRefresh[granularity.ordinal]
//        if (forceRefresh) {
//            topPerformersForceRefresh[granularity.ordinal] = false
//        }
//
//        myStoreView?.showTopPerformersSkeleton(true)
//        coroutineScope.launch {
//            getTopPerformers(forceRefresh, granularity, NUM_TOP_PERFORMERS)
//                .collect {
//                    myStoreView?.showTopPerformersSkeleton(false)
//                    when (it) {
//                        is TopPerformersSuccess -> {
//                            myStoreView?.showTopPerformers(it.topPerformers, granularity)
//                            AnalyticsTracker.track(
//                                AnalyticsTracker.Stat.DASHBOARD_TOP_PERFORMERS_LOADED,
//                                mapOf(AnalyticsTracker.KEY_RANGE to granularity.name.lowercase())
//                            )
//                        }
//                        TopPerformersError -> myStoreView?.showTopPerformersError(granularity)
//                    }
//                }
//        }
//    }

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
