package com.woocommerce.android.aiassistant.ui.cards

data class AiAssistantStatsCardState(
    val period: String,
    val revenueTotal: String,
    val orderCount: String,
    val chartValues: List<Double>,
    val isTrendAvailable: Boolean,
)
