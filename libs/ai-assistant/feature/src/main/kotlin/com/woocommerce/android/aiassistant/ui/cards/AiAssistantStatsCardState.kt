package com.woocommerce.android.aiassistant.ui.cards

data class AiAssistantStatsCardState(
    val period: String,
    val metrics: List<Metric>,
) {
    data class Metric(
        val type: AssistantCard.Stats.MetricType,
        val value: String,
        val chartValues: List<Double>,
    )
}
