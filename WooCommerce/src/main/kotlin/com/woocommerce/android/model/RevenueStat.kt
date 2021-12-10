package com.woocommerce.android.model

data class RevenueStat(
    val totalValue: Double,
    val totalDelta: Double,
    val netValue: Double,
    val netDelta: Double,
    val currencyCode: String?
)
