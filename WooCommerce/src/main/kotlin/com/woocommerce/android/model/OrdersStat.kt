package com.woocommerce.android.model

data class OrdersStat(
    val ordersCount: Int,
    val ordersCountDelta: DeltaPercentage,
    val avgOrderValue: Double,
    val avgOrderDelta: DeltaPercentage,
    val currencyCode: String?,
    val ordersCountByInterval: List<Long>,
    val avgOrderValueByInterval: List<Double>
) {
    companion object {
        val EMPTY = OrdersStat(
            ordersCount = 0,
            ordersCountDelta = DeltaPercentage.NotExist,
            avgOrderValue = 0.0,
            avgOrderDelta = DeltaPercentage.NotExist,
            currencyCode = null,
            ordersCountByInterval = emptyList(),
            avgOrderValueByInterval = emptyList()
        )
    }
}
