package com.woocommerce.android.ui.mystore

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import com.woocommerce.android.ui.mystore.MyStoreViewModel.TopPerformerProductUiModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface MyStoreContract {
    interface Presenter : BasePresenter<View> {
        fun loadStats(granularity: StatsGranularity, forced: Boolean = false)
        fun getStatsCurrency(): String?
        fun getSelectedSiteName(): String?
        fun dismissJetpackBenefitsBanner()
    }

    interface View : BaseView<Presenter> {
        var isRefreshPending: Boolean

        fun refreshMyStoreStats(forced: Boolean = false)
        fun showStats(revenueStatsModel: WCRevenueStatsModel?, granularity: StatsGranularity)
        fun showStatsError(granularity: StatsGranularity)
        fun updateStatsAvailabilityError()
        fun showTopPerformers(topPerformers: List<TopPerformerProductUiModel>, granularity: StatsGranularity)
        fun showTopPerformersError(granularity: StatsGranularity)
        fun showVisitorStats(visitorStats: Map<String, Int>, granularity: StatsGranularity)
        fun showVisitorStatsError(granularity: StatsGranularity)
        fun showEmptyVisitorStatsForJetpackCP()
        fun showErrorSnack()
        fun showEmptyView(show: Boolean)
        fun showJetpackBenefitsBanner(show: Boolean)

        fun showChartSkeleton(show: Boolean)
    }
}
