package com.woocommerce.android.model
@Suppress("LongParameterList")
class GiftCardsStat(
    val usedValue: Long,
    val usedDelta: DeltaPercentage,
    val netValue: Double,
    val netDelta: DeltaPercentage,
    val currencyCode: String?,
    val usedByInterval: List<Long>,
    val netRevenueByInterval: List<Double>
) {
    companion object {
        val EMPTY = GiftCardsStat(
            usedValue = 0L,
            usedDelta = DeltaPercentage.NotExist,
            netValue = 0.0,
            netDelta = DeltaPercentage.NotExist,
            currencyCode = null,
            usedByInterval = emptyList(),
            netRevenueByInterval = emptyList()
        )
    }
}
