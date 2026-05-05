package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantAnalyticsRevenueCardParserTest {
    private val parser = AssistantAnalyticsRevenueCardParser(
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        }
    )

    @Test
    fun `given analytics revenue result, when parsed, then stats card contains date range metrics and chart points`() {
        val card = parser.parse(success())

        assertThat(card).isEqualTo(
            AssistantCard.Stats(
                after = "2026-05-01",
                before = "2026-05-07",
                revenueTotal = "123.45",
                revenueCurrency = "USD",
                orderCount = "8",
                chartPoints = listOf(
                    AssistantCard.Stats.ChartPoint(date = "2026-05-01", value = 12.0),
                    AssistantCard.Stats.ChartPoint(date = "2026-05-02", value = 18.0),
                ),
            )
        )
    }

    @Test
    fun `given numeric totals and chart values, when parsed, then values are preserved`() {
        val card = parser.parse(success(revenueTotal = 123.45, orderCount = 8, firstChartValue = 0.0))

        assertThat(card?.revenueTotal).isEqualTo("123.45")
        assertThat(card?.orderCount).isEqualTo("8")
        assertThat(card?.chartPoints?.first()?.value).isEqualTo(0.0)
    }

    @Test
    fun `given missing totals, when parsed, then metric fields are empty and chart points are still parsed`() {
        val card = parser.parse(success(includeTotals = false))

        assertThat(card?.revenueTotal).isEmpty()
        assertThat(card?.orderCount).isEmpty()
        assertThat(card?.chartPoints).hasSize(2)
    }

    @Test
    fun `given empty revenue chart, when parsed, then chart points are empty`() {
        val card = parser.parse(success(chart = buildJsonArray {}))

        assertThat(card?.chartPoints).isEmpty()
    }

    @Test
    fun `given single revenue chart point, when parsed, then one chart point is preserved`() {
        val card = parser.parse(success(chart = buildJsonArray { addChartPoint("2026-05-01", 12.0) }))

        assertThat(card?.chartPoints).containsExactly(AssistantCard.Stats.ChartPoint("2026-05-01", 12.0))
    }

    @Test
    fun `given all zero revenue chart points, when parsed, then zero points are preserved`() {
        val card = parser.parse(
            success(
                chart = buildJsonArray {
                    addChartPoint("2026-05-01", 0.0)
                    addChartPoint("2026-05-02", 0.0)
                }
            )
        )

        assertThat(card?.chartPoints?.map { it.value }).containsExactly(0.0, 0.0)
    }

    @Test
    fun `given negative revenue chart points, when parsed, then negative points are preserved`() {
        val card = parser.parse(success(chart = buildJsonArray { addChartPoint("2026-05-01", -5.0) }))

        assertThat(card?.chartPoints?.single()?.value).isEqualTo(-5.0)
    }

    @Test
    fun `given non numeric chart value, when parsed, then malformed point is skipped`() {
        val malformedPoint = buildJsonObject {
            put("date", "2026-05-01")
            put("value", "not-a-number")
        }
        val card = parser.parse(
            success(
                chart = buildJsonArray {
                    add(malformedPoint)
                    addChartPoint("2026-05-02", 8.0)
                }
            )
        )

        assertThat(card?.chartPoints).containsExactly(AssistantCard.Stats.ChartPoint("2026-05-02", 8.0))
    }

    @Test
    fun `given malformed chart date, when parsed, then malformed point is skipped`() {
        val card = parser.parse(
            success(
                chart = buildJsonArray {
                    addChartPoint("2026-99-99", 12.0)
                    addChartPoint("2026-05-02", 8.0)
                }
            )
        )

        assertThat(card?.chartPoints).containsExactly(AssistantCard.Stats.ChartPoint("2026-05-02", 8.0))
    }

    @Test
    fun `given missing after or before, when parsed, then no card is returned`() {
        assertThat(parser.parse(success(after = null))).isNull()
        assertThat(parser.parse(success(before = null))).isNull()
    }

    private fun success(
        after: String? = "2026-05-01",
        before: String? = "2026-05-07",
        revenueTotal: Any = "123.45",
        orderCount: Any = "8",
        firstChartValue: Any = 12.0,
        includeTotals: Boolean = true,
        chart: kotlinx.serialization.json.JsonArray = buildJsonArray {
            addChartPoint("2026-05-01", firstChartValue)
            addChartPoint("2026-05-02", 18.0)
        },
    ) = ToolResult.Success(
        toolCallId = "call-1",
        structured = buildJsonObject {
            after?.let { put("after", it) }
            before?.let { put("before", it) }
            put("currency", "USD")
            if (includeTotals) {
                put(
                    "totals",
                    buildJsonObject {
                        putAny("net_revenue", revenueTotal)
                        putAny("orders_count", orderCount)
                    }
                )
            }
            put("revenue_chart", chart)
        },
    )

    private fun kotlinx.serialization.json.JsonArrayBuilder.addChartPoint(date: String, value: Any) {
        add(
            buildJsonObject {
                put("date", date)
                putAny("value", value)
            }
        )
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putAny(name: String, value: Any) {
        when (value) {
            is Double -> put(name, value)
            is Int -> put(name, value)
            is String -> put(name, value)
            else -> error("Unsupported test value $value")
        }
    }
}
