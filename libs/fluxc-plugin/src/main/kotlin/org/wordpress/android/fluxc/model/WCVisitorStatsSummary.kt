package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

data class WCVisitorStatsSummary(
    val granularity: StatsGranularity,
    val date: String,
    val views: Int,
    val visitors: Int
)
