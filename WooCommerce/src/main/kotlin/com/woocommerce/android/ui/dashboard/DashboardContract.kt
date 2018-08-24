package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface DashboardContract {
    interface Presenter : BasePresenter<View> {
        fun loadStats(granularity: StatsGranularity, forced: Boolean = false)
        fun loadTopEarnerStats(granularity: StatsGranularity, forced: Boolean = false)
        fun resetTopEarnersForceRefresh()
        fun getStatsCurrency(): String?
        fun fetchUnfilledOrderCount()
    }

    interface View : BaseView<Presenter> {
        var isActive: Boolean

        fun refreshDashboard()
        fun setLoadingIndicator(active: Boolean)
        fun showStats(revenueStats: Map<String, Double>, salesStats: Map<String, Int>, granularity: StatsGranularity)
        fun showStatsError(granularity: StatsGranularity)
        fun showTopEarners(topEarnerList: List<WCTopEarnerModel>, granularity: StatsGranularity)
        fun showTopEarnersError(granularity: StatsGranularity)
        fun showErrorSnack()
        fun hideUnfilledOrdersCard()
        fun showUnfilledOrdersCard(count: Int, canLoadMore: Boolean)
        fun showUnfilledOrdersProgress(show: Boolean)
    }
}
