package com.woocommerce.android.model

data class OrdersStat(
    val ordersCount: Int,
    val ordersCountDelta: DeltaPercentage,
    val avgOrderValue: Double,
    val avgOrderDelta: DeltaPercentage,
    val currencyCode: String?,
    val ordersCountByInterval: List<Long>,
    val avgOrderValueByInterval: List<Double>
)
