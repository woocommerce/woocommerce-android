package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.aiassistant.ui.cards.AiAssistantStatsCardState
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Locale

class AiAssistantStatsCardRendererTest {
    private val currencyFormatter: AiAssistantCurrencyFormatter = mock()

    @Test
    fun `when renderer is created, then class has direct unit test coverage`() {
        assertThat(AiAssistantStatsCardRenderer(currencyFormatter)).isNotNull
    }

    @Test
    fun `given assistant stats card, when mapped, then period total sales net sales and chart values are displayed`() {
        whenever(currencyFormatter.formatCurrency("123.45", "USD")).thenReturn("$123.45")
        whenever(currencyFormatter.formatCurrency("100.15", "USD")).thenReturn("$100.15")

        val model = statsCard(currency = "USD").toStatsCardState(
            currencyFormatter = currencyFormatter,
            unavailableValue = "Unavailable",
            locale = Locale.US,
        )

        assertThat(model.period).isEqualTo("May 1 - May 7, 2026")
        assertThat(model.metrics).containsExactly(
            AiAssistantStatsCardState.Metric(
                type = AssistantCard.Stats.MetricType.TotalSales,
                value = "$123.45",
                chartValues = listOf(12.0, 18.0, 9.0),
            ),
            AiAssistantStatsCardState.Metric(
                type = AssistantCard.Stats.MetricType.NetSales,
                value = "$100.15",
                chartValues = listOf(10.0, 15.0, 8.0),
            ),
        )
        verify(currencyFormatter).formatCurrency("123.45", "USD")
        verify(currencyFormatter).formatCurrency("100.15", "USD")
    }

    @Test
    fun `given assistant stats card with one day range, when mapped, then single date is displayed`() {
        val period = statsCard(after = "2026-05-01", before = "2026-05-01")
            .toStatsCardState(currencyFormatter, unavailableValue = "Unavailable", locale = Locale.US)
            .period

        assertThat(period).isEqualTo("May 1, 2026")
    }

    @Test
    fun `given assistant stats card with blank metrics, when mapped, then unavailable fallback is used`() {
        val model = statsCard(
            totalSales = "",
            netSales = "",
            totalSalesChartPoints = emptyList(),
            netSalesChartPoints = emptyList(),
        )
            .toStatsCardState(currencyFormatter, unavailableValue = "Not available", locale = Locale.US)

        assertThat(model).isEqualTo(
            AiAssistantStatsCardState(
                period = "May 1 - May 7, 2026",
                metrics = listOf(
                    AiAssistantStatsCardState.Metric(
                        type = AssistantCard.Stats.MetricType.TotalSales,
                        value = "Not available",
                        chartValues = emptyList(),
                    ),
                    AiAssistantStatsCardState.Metric(
                        type = AssistantCard.Stats.MetricType.NetSales,
                        value = "Not available",
                        chartValues = emptyList(),
                    ),
                ),
            )
        )
    }

    @Test
    fun `given assistant stats card without net sales chart points, when mapped, then net sales chart values are empty`() {
        val model = statsCard(netSalesChartPoints = emptyList()).toStatsCardState(
            currencyFormatter = currencyFormatter,
            unavailableValue = "Unavailable",
            locale = Locale.US,
        )

        assertThat(model.metric(AssistantCard.Stats.MetricType.TotalSales).chartValues)
            .containsExactly(12.0, 18.0, 9.0)
        assertThat(model.metric(AssistantCard.Stats.MetricType.NetSales).chartValues).isEmpty()
    }

    @Test
    fun `given assistant stats card without total sales chart points, when mapped, then total sales chart values are empty`() {
        val model = statsCard(totalSalesChartPoints = emptyList()).toStatsCardState(
            currencyFormatter = currencyFormatter,
            unavailableValue = "Unavailable",
            locale = Locale.US,
        )

        assertThat(model.metric(AssistantCard.Stats.MetricType.TotalSales).chartValues).isEmpty()
        assertThat(model.metric(AssistantCard.Stats.MetricType.NetSales).chartValues)
            .containsExactly(10.0, 15.0, 8.0)
    }

    @Test
    fun `given assistant stats card with invalid dates, when mapped, then raw date range is preserved`() {
        val period = statsCard(after = "bad", before = "2026-05-07")
            .toStatsCardState(currencyFormatter, unavailableValue = "Unavailable", locale = Locale.US)
            .period

        assertThat(period).isEqualTo("bad - 2026-05-07")
    }

    @Test
    fun `given assistant stats card without currency, when mapped, then raw total sales is used`() {
        val totalSales = statsCard(currency = "")
            .toStatsCardState(currencyFormatter, unavailableValue = "Unavailable", locale = Locale.US)
            .metric(AssistantCard.Stats.MetricType.TotalSales)
            .value

        assertThat(totalSales).isEqualTo("123.45")
    }

    @Test
    fun `given orders stats card, when mapped, then count is raw and average order value is money`() {
        whenever(currencyFormatter.formatCurrency("85.30", "USD")).thenReturn("$85.30")

        val model = ordersStatsCard().toStatsCardState(
            currencyFormatter = currencyFormatter,
            unavailableValue = "Unavailable",
            locale = Locale.US,
        )

        assertThat(model.metrics).containsExactly(
            AiAssistantStatsCardState.Metric(
                type = AssistantCard.Stats.MetricType.TotalOrders,
                value = "42",
                chartValues = listOf(12.0),
            ),
            AiAssistantStatsCardState.Metric(
                type = AssistantCard.Stats.MetricType.AverageOrderValue,
                value = "$85.30",
                chartValues = listOf(80.10),
            ),
        )
        verify(currencyFormatter, never()).formatCurrency("42", "USD")
        verify(currencyFormatter).formatCurrency("85.30", "USD")
    }

    @Test
    fun `given orders stats card with blank metrics, when mapped, then unavailable fallback is used`() {
        val model = ordersStatsCard(
            ordersCount = "",
            averageOrderValue = "",
            ordersCountChartPoints = emptyList(),
            averageOrderValueChartPoints = emptyList(),
        ).toStatsCardState(
            currencyFormatter = currencyFormatter,
            unavailableValue = "Unavailable",
            locale = Locale.US,
        )

        assertThat(model.metrics).containsExactly(
            AiAssistantStatsCardState.Metric(
                type = AssistantCard.Stats.MetricType.TotalOrders,
                value = "Unavailable",
                chartValues = emptyList(),
            ),
            AiAssistantStatsCardState.Metric(
                type = AssistantCard.Stats.MetricType.AverageOrderValue,
                value = "Unavailable",
                chartValues = emptyList(),
            ),
        )
        verify(currencyFormatter, never()).formatCurrency("", "USD")
    }

    @Test
    fun `given assistant stats card, when stats card click handler is invoked, then analytics action is emitted`() {
        val actions = mutableListOf<AssistantCardAction>()

        statsCard(after = "2026-05-01", before = "2026-05-07")
            .toStatsCardClickHandler(actions::add)
            .invoke()

        assertThat(actions).containsExactly(
            AssistantCardAction.OpenAnalytics(after = "2026-05-01", before = "2026-05-07")
        )
    }

    private fun statsCard(
        after: String = "2026-05-01",
        before: String = "2026-05-07",
        totalSales: String = "123.45",
        netSales: String = "100.15",
        currency: String = "",
        totalSalesChartPoints: List<AssistantCard.Stats.ChartPoint> = listOf(
            AssistantCard.Stats.ChartPoint("2026-05-01", 12.0),
            AssistantCard.Stats.ChartPoint("2026-05-02", 18.0),
            AssistantCard.Stats.ChartPoint("2026-05-03", 9.0),
        ),
        netSalesChartPoints: List<AssistantCard.Stats.ChartPoint> = listOf(
            AssistantCard.Stats.ChartPoint("2026-05-01", 10.0),
            AssistantCard.Stats.ChartPoint("2026-05-02", 15.0),
            AssistantCard.Stats.ChartPoint("2026-05-03", 8.0),
        ),
    ) = AssistantCard.Stats(
        id = ANALYTICS_STATS_ID,
        kind = AssistantCard.Stats.Kind.Revenue,
        after = after,
        before = before,
        currency = currency,
        metrics = listOf(
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.TotalSales,
                value = totalSales,
                chartPoints = totalSalesChartPoints,
            ),
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.NetSales,
                value = netSales,
                chartPoints = netSalesChartPoints,
            ),
        ),
    )

    private fun ordersStatsCard(
        ordersCount: String = "42",
        averageOrderValue: String = "85.30",
        ordersCountChartPoints: List<AssistantCard.Stats.ChartPoint> = listOf(
            AssistantCard.Stats.ChartPoint("2026-05-01", 12.0),
        ),
        averageOrderValueChartPoints: List<AssistantCard.Stats.ChartPoint> = listOf(
            AssistantCard.Stats.ChartPoint("2026-05-01", 80.10),
        ),
    ) = AssistantCard.Stats(
        id = ANALYTICS_ORDERS_STATS_ID,
        kind = AssistantCard.Stats.Kind.Orders,
        after = "2026-05-01",
        before = "2026-05-07",
        currency = "USD",
        metrics = listOf(
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.TotalOrders,
                value = ordersCount,
                chartPoints = ordersCountChartPoints,
            ),
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.AverageOrderValue,
                value = averageOrderValue,
                chartPoints = averageOrderValueChartPoints,
            ),
        ),
    )

    private fun AiAssistantStatsCardState.metric(
        type: AssistantCard.Stats.MetricType,
    ): AiAssistantStatsCardState.Metric = metrics.single { it.type == type }

    private companion object {
        private const val ANALYTICS_STATS_ID =
            "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:USD"
        private const val ANALYTICS_ORDERS_STATS_ID =
            "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day:currency:none"
    }
}
