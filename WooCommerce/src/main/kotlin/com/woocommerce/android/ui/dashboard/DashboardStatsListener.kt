package com.woocommerce.android.ui.dashboard

import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

interface DashboardStatsListener {
    fun loadStats(period: StatsGranularity)
}
