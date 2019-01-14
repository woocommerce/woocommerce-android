package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface DashboardContract {
    interface Presenter : BasePresenter<View> {
        fun loadStats(granularity: StatsGranularity, forced: Boolean = false)
        fun loadTopEarnerStats(granularity: StatsGranularity, forced: Boolean = false)
        fun resetForceRefresh()
        fun getStatsCurrency(): String?
        fun fetchUnfilledOrderCount(forced: Boolean = false)
        fun fetchHasOrders()
        fun checkApiVersion()
    }

    interface View : BaseView<Presenter> {
        var isActive: Boolean
        var isRefreshPending: Boolean

        fun refreshDashboard(forced: Boolean = false)
        fun showStats(revenueStats: Map<String, Double>, salesStats: Map<String, Int>, granularity: StatsGranularity)
        fun showStatsError(granularity: StatsGranularity)
        fun showTopEarners(topEarnerList: List<WCTopEarnerModel>, granularity: StatsGranularity)
        fun showTopEarnersError(granularity: StatsGranularity)
        fun showVisitorStats(visits: Int, granularity: StatsGranularity)
        fun showVisitorStatsError(granularity: StatsGranularity)
        fun showErrorSnack()
        fun hideUnfilledOrdersCard()
        fun showUnfilledOrdersCard(count: Int)
        fun showPluginVersionNoticeCard()
        fun hidePluginVersionNoticeCard()
        fun showNoOrdersView(show: Boolean)

        fun showChartSkeleton(show: Boolean)
        fun showUnfilledOrdersSkeleton(show: Boolean)
        fun showTopEarnersSkeleton(show: Boolean)
    }
}
