package com.woocommerce.android.ui.dashboard

import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface DashboardStatsListener {
    fun onRequestLoadStats(period: StatsGranularity)
    fun onRequestLoadTopEarnerStats(period: StatsGranularity)
    fun onRequestLoadCustomStats(wcOrderStatsModel: WCOrderStatsModel?)
}
