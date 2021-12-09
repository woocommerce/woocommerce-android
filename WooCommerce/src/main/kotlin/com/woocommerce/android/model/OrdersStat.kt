package com.woocommerce.android.model

data class OrdersStat(
    val ordersCount: Int,
    val ordersCountDelta: Int,
    val avgOrderValue: Double,
    val avgOrderDelta: Int,
    val currencyCode: String?
)
