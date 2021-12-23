package com.woocommerce.android.ui.mystore

import com.woocommerce.android.ui.mystore.MyStoreContract.Presenter
import com.woocommerce.android.ui.mystore.MyStoreContract.View
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@ExperimentalCoroutinesApi
class MyStorePresenter @Inject constructor(
) : Presenter {

    private var myStoreView: View? = null

    override fun takeView(view: View) {
    }

    override fun dropView() {
        super.dropView()
    }

    override fun getStatsCurrency(): String? {
        TODO("Not yet implemented")
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

    override fun getSelectedSiteName(): String = ""

    override fun dismissJetpackBenefitsBanner() {
    }
}
