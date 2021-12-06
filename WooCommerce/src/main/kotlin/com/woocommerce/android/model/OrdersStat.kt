package com.woocommerce.android.model

data class OrdersStat(
    val totalOrders: Int,
    val totalDelta: Int,
    val avgOrderValue: Double,
    val avgOrderDelta: Int,
    val currencyCode: String?
)
