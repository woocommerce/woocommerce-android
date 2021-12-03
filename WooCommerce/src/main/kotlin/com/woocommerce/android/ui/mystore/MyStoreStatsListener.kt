package com.woocommerce.android.ui.mystore

import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface MyStoreStatsListener {
    fun onRequestLoadStats(period: StatsGranularity)
    fun onChartValueSelected(dateString: String, period: StatsGranularity) {}
    fun onChartValueUnSelected(revenueStatsModel: WCRevenueStatsModel?, period: StatsGranularity) {}
}
