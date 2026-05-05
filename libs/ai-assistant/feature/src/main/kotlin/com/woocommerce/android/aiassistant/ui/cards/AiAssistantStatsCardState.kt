package com.woocommerce.android.aiassistant.ui.cards

data class AiAssistantStatsCardState(
    val period: String,
    val revenueTotal: String,
    val orderCount: String,
    val revenueChartValues: List<Double>,
    val orderChartValues: List<Double>,
    val isRevenueTrendAvailable: Boolean,
    val isOrdersTrendAvailable: Boolean,
)
