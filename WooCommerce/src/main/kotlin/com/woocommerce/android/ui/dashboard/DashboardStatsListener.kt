package com.woocommerce.android.ui.dashboard

import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface DashboardStatsListener {
    fun onRequestLoadStats(period: StatsGranularity)
    fun onRequestLoadTopEarnerStats(period: StatsGranularity)
    fun onTopEarnerClicked(topEarner: WCTopEarnerModel)
    fun onChartValueSelected(dateString: String, period: StatsGranularity) {}
    fun onChartValueUnSelected(revenueStatsModel: WCRevenueStatsModel?, period: StatsGranularity) {}
}
