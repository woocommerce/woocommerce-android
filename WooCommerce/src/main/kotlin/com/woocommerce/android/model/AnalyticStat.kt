package com.woocommerce.android.model

sealed class AnalyticStat {
    data class RevenueStat(
        val totalValue: Double,
        val totalDelta: Int,
        val netValue: Double,
        val netDelta: Int,
        val currencyCode: String?
    ) : AnalyticStat()
}
