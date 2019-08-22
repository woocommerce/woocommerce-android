package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface DashboardContract {
    interface Presenter : BasePresenter<View> {
        fun loadStats(granularity: StatsGranularity, forced: Boolean = false)
        fun loadTopEarnerStats(granularity: StatsGranularity, forced: Boolean = false)
        fun getStatsCurrency(): String?
        fun fetchHasOrders()
    }

    interface View : BaseView<Presenter> {
        var isRefreshPending: Boolean

        fun refreshDashboard(forced: Boolean = false)
        fun showStats(revenueStats: Map<String, Double>, salesStats: Map<String, Int>, granularity: StatsGranularity)
        fun showStatsError(granularity: StatsGranularity)
        fun showTopEarners(topEarnerList: List<WCTopEarnerModel>, granularity: StatsGranularity)
        fun showTopEarnersError(granularity: StatsGranularity)
        fun showVisitorStats(visitorStats: Map<String, Int>, granularity: StatsGranularity)
        fun showVisitorStatsError(granularity: StatsGranularity)
        fun showErrorSnack()
        fun showEmptyView(show: Boolean)

        fun showChartSkeleton(show: Boolean)
        fun showTopEarnersSkeleton(show: Boolean)

        fun showV4StatsRevertedBanner(show: Boolean)
    }
}
