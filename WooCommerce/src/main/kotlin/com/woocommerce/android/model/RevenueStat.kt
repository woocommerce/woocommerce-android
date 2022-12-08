package com.woocommerce.android.model

data class RevenueStat(
    val totalValue: Double,
    val totalDelta: DeltaPercentage,
    val netValue: Double,
    val netDelta: DeltaPercentage,
    val currencyCode: String?,
    val totalRevenueByInterval: List<Double>,
    val netRevenueByInterval: List<Double>
)

sealed class DeltaPercentage {
    data class Value(val value: Int) : DeltaPercentage()
    object NotExist : DeltaPercentage()
}
