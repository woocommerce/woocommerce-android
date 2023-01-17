package com.woocommerce.android.model

data class RevenueStat(
    val totalValue: Double,
    val totalDelta: DeltaPercentage,
    val netValue: Double,
    val netDelta: DeltaPercentage,
    val currencyCode: String?,
    val totalRevenueByInterval: List<Double>,
    val netRevenueByInterval: List<Double>
) {
    companion object {
        val EMPTY = RevenueStat(
            totalValue = 0.0,
            totalDelta = DeltaPercentage.NotExist,
            netValue = 0.0,
            netDelta = DeltaPercentage.NotExist,
            currencyCode = null,
            totalRevenueByInterval = emptyList(),
            netRevenueByInterval = emptyList()
        )
    }
}

sealed class DeltaPercentage {
    data class Value(val value: Int) : DeltaPercentage()
    object NotExist : DeltaPercentage()
}
