package com.woocommerce.android.aiassistant.ui.cards

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AiAssistantStatsCardTest {
    @Test
    fun `given empty values, when deciding trend visibility, then chart is hidden`() {
        assertThat(shouldShowStatsTrendChart(emptyList())).isFalse()
    }

    @Test
    fun `given one value, when deciding trend visibility, then chart is hidden`() {
        assertThat(shouldShowStatsTrendChart(listOf(12.0))).isFalse()
    }

    @Test
    fun `given multiple values, when deciding trend visibility, then chart is shown`() {
        assertThat(shouldShowStatsTrendChart(listOf(12.0, 18.0))).isTrue()
    }

    @Test
    fun `given only later right-column metric has trend, when building trend rows, then column is preserved`() {
        val rows = statsTrendRows(
            listOf(
                metric(AssistantCard.Stats.MetricType.TotalSales, chartValues = emptyList()),
                metric(AssistantCard.Stats.MetricType.NetSales, chartValues = emptyList()),
                metric(AssistantCard.Stats.MetricType.TotalOrders, chartValues = emptyList()),
                metric(AssistantCard.Stats.MetricType.AverageOrderValue, chartValues = listOf(80.10, 91.20)),
            )
        )

        assertThat(rows).hasSize(1)
        assertThat(rows.single()).hasSize(2)
        assertThat(rows.single()[0]).isNull()
        assertThat(rows.single()[1]?.type).isEqualTo(AssistantCard.Stats.MetricType.AverageOrderValue)
    }

    private fun metric(
        type: AssistantCard.Stats.MetricType,
        chartValues: List<Double>,
    ) = AiAssistantStatsCardState.Metric(
        type = type,
        value = "1",
        chartValues = chartValues,
    )
}
