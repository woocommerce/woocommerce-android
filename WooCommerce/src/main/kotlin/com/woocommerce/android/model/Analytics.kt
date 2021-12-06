package com.woocommerce.android.model

data class RevenueStat(
    val totalValue: Double,
    val totalDelta: Int,
    val netValue: Double,
    val netDelta: Int,
    val currencyCode: String?
)
