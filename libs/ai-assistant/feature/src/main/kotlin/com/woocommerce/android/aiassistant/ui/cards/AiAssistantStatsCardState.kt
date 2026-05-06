package com.woocommerce.android.aiassistant.ui.cards

data class AiAssistantStatsCardState(
    val period: String,
    val totalSales: String,
    val netSales: String,
    val totalSalesChartValues: List<Double>,
    val netSalesChartValues: List<Double>,
    val isTotalSalesTrendAvailable: Boolean,
    val isNetSalesTrendAvailable: Boolean,
)
