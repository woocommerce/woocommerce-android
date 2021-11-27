package com.woocommerce.android.model

sealed class AnalyticStat {
    data class RevenueStat(
        val total: Double,
        val totalDelta: Int,
        val net: Double,
        val netDelta: Int
    ) : AnalyticStat()
}
