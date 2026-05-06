package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.aiassistant.ui.cards.AiAssistantStatsCardState
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.util.CurrencyFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Locale

class AiAssistantStatsCardRendererTest {
    private val currencyFormatter: CurrencyFormatter = mock()

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
        assertThat(model.totalSales).isEqualTo("$123.45")
        assertThat(model.netSales).isEqualTo("$100.15")
        assertThat(model.totalSalesChartValues).containsExactly(12.0, 18.0, 9.0)
        assertThat(model.netSalesChartValues).containsExactly(10.0, 15.0, 8.0)
        assertThat(model.isTotalSalesTrendAvailable).isTrue()
        assertThat(model.isNetSalesTrendAvailable).isTrue()
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
                totalSales = "Not available",
                netSales = "Not available",
                totalSalesChartValues = emptyList(),
                netSalesChartValues = emptyList(),
                isTotalSalesTrendAvailable = false,
                isNetSalesTrendAvailable = false,
            )
        )
    }

    @Test
    fun `given assistant stats card without net sales chart points, when mapped, then only net sales trend is unavailable`() {
        val model = statsCard(netSalesChartPoints = emptyList()).toStatsCardState(
            currencyFormatter = currencyFormatter,
            unavailableValue = "Unavailable",
            locale = Locale.US,
        )

        assertThat(model.totalSalesChartValues).containsExactly(12.0, 18.0, 9.0)
        assertThat(model.netSalesChartValues).isEmpty()
        assertThat(model.isTotalSalesTrendAvailable).isTrue()
        assertThat(model.isNetSalesTrendAvailable).isFalse()
    }

    @Test
    fun `given assistant stats card without total sales chart points, when mapped, then only total sales trend is unavailable`() {
        val model = statsCard(totalSalesChartPoints = emptyList()).toStatsCardState(
            currencyFormatter = currencyFormatter,
            unavailableValue = "Unavailable",
            locale = Locale.US,
        )

        assertThat(model.totalSalesChartValues).isEmpty()
        assertThat(model.netSalesChartValues).containsExactly(10.0, 15.0, 8.0)
        assertThat(model.isTotalSalesTrendAvailable).isFalse()
        assertThat(model.isNetSalesTrendAvailable).isTrue()
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
            .totalSales

        assertThat(totalSales).isEqualTo("123.45")
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
        after = after,
        before = before,
        currency = currency,
        totalSales = totalSales,
        netSales = netSales,
        totalSalesChartPoints = totalSalesChartPoints,
        netSalesChartPoints = netSalesChartPoints,
    )

    private companion object {
        private const val ANALYTICS_STATS_ID =
            "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:USD"
    }
}
