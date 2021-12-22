package com.woocommerce.android.ui.mystore

import com.woocommerce.android.ui.mystore.MyStoreViewModel.RevenueStatsUiModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface MyStoreStatsListener {
    fun onChartValueSelected(dateString: String, period: StatsGranularity) {}
    fun onChartValueUnSelected(revenueStatsModel: RevenueStatsUiModel?, period: StatsGranularity) {}
}
