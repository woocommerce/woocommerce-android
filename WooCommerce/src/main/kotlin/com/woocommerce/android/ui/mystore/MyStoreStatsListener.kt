package com.woocommerce.android.ui.mystore

import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface MyStoreStatsListener {
    fun onChartValueSelected(dateString: String, period: StatsGranularity) {}
    fun onChartValueUnSelected(revenueStatsModel: RevenueStatsUiModel?, period: StatsGranularity) {}
}
