package com.woocommerce.android.ui.mystore

import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface MyStoreStatsListener {
    fun onTopPerformerClicked(topPerformer: WCTopPerformerProductModel)
    fun onRequestLoadStats(period: StatsGranularity)
    fun onRequestLoadTopEarnerStats(period: StatsGranularity)
    fun onChartValueSelected(dateString: String, period: StatsGranularity) {}
    fun onChartValueUnSelected(revenueStatsModel: WCRevenueStatsModel?, period: StatsGranularity) {}
}
