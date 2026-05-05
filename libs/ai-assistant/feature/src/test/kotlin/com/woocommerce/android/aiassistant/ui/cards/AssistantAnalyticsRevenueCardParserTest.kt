package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
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
                revenueChartPoints = listOf(
                    AssistantCard.Stats.ChartPoint(date = "2026-05-01", value = 12.0),
                    AssistantCard.Stats.ChartPoint(date = "2026-05-02", value = 18.0),
                ),
                orderChartPoints = listOf(
                    AssistantCard.Stats.ChartPoint(date = "2026-05-01", value = 1.0),
                    AssistantCard.Stats.ChartPoint(date = "2026-05-02", value = 3.0),
                ),
            )
        )
    }

    @Test
    fun `given numeric totals and chart values, when parsed, then values are preserved`() {
        val card = parser.parse(success(revenueTotal = 123.45, orderCount = 8, firstChartValue = 0.0))

        assertThat(card?.revenueTotal).isEqualTo("123.45")
        assertThat(card?.orderCount).isEqualTo("8")
        assertThat(card?.revenueChartPoints?.first()?.value).isEqualTo(0.0)
    }

    @Test
    fun `given missing totals, when parsed, then metric fields are empty and chart points are still parsed`() {
        val card = parser.parse(success(includeTotals = false))

        assertThat(card?.revenueTotal).isEmpty()
        assertThat(card?.orderCount).isEmpty()
        assertThat(card?.revenueChartPoints).hasSize(2)
        assertThat(card?.orderChartPoints).hasSize(2)
    }

    @Test
    fun `given null primary totals and fallback totals, when parsed, then fallback metrics are used`() {
        val card = parser.parse(
            success(
                totals = buildJsonObject {
                    put("net_revenue", JsonNull)
                    put("total_sales", "99.50")
                    put("orders_count", JsonNull)
                    put("orders", 7)
                }
            )
        )

        assertThat(card?.revenueTotal).isEqualTo("99.50")
        assertThat(card?.orderCount).isEqualTo("7")
    }

    @Test
    fun `given empty revenue chart, when parsed, then chart points are empty`() {
        val card = parser.parse(success(revenueChart = buildJsonArray {}, orderChart = buildJsonArray {}))

        assertThat(card?.revenueChartPoints).isEmpty()
        assertThat(card?.orderChartPoints).isEmpty()
    }

    @Test
    fun `given single revenue chart point, when parsed, then one chart point is preserved`() {
        val card = parser.parse(success(revenueChart = buildJsonArray { addChartPoint("2026-05-01", 12.0) }))

        assertThat(card?.revenueChartPoints).containsExactly(AssistantCard.Stats.ChartPoint("2026-05-01", 12.0))
    }

    @Test
    fun `given all zero revenue chart points, when parsed, then zero points are preserved`() {
        val card = parser.parse(
            success(
                revenueChart = buildJsonArray {
                    addChartPoint("2026-05-01", 0.0)
                    addChartPoint("2026-05-02", 0.0)
                }
            )
        )

        assertThat(card?.revenueChartPoints?.map { it.value }).containsExactly(0.0, 0.0)
    }

    @Test
    fun `given negative revenue chart points, when parsed, then negative points are preserved`() {
        val card = parser.parse(success(revenueChart = buildJsonArray { addChartPoint("2026-05-01", -5.0) }))

        assertThat(card?.revenueChartPoints?.single()?.value).isEqualTo(-5.0)
    }

    @Test
    fun `given non numeric chart values, when parsed, then malformed points are skipped per series`() {
        val malformedPoint = buildJsonObject {
            put("date", "2026-05-01")
            put("value", "not-a-number")
        }
        val card = parser.parse(
            success(
                revenueChart = buildJsonArray {
                    add(malformedPoint)
                    addChartPoint("2026-05-02", 8.0)
                },
                orderChart = buildJsonArray {
                    addChartPoint("2026-05-01", 1.0)
                    add(malformedPoint)
                },
            )
        )

        assertThat(card?.revenueChartPoints).containsExactly(AssistantCard.Stats.ChartPoint("2026-05-02", 8.0))
        assertThat(card?.orderChartPoints).containsExactly(AssistantCard.Stats.ChartPoint("2026-05-01", 1.0))
    }

    @Test
    fun `given malformed chart date, when parsed, then malformed point is skipped`() {
        val card = parser.parse(
            success(
                revenueChart = buildJsonArray {
                    addChartPoint("2026-99-99", 12.0)
                    addChartPoint("2026-05-02", 8.0)
                }
            )
        )

        assertThat(card?.revenueChartPoints).containsExactly(AssistantCard.Stats.ChartPoint("2026-05-02", 8.0))
    }

    @Test
    fun `given missing order chart, when parsed, then order chart points are empty and revenue points remain`() {
        val card = parser.parse(success(orderChart = null))

        assertThat(card?.revenueChartPoints).hasSize(2)
        assertThat(card?.orderChartPoints).isEmpty()
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
        totals: kotlinx.serialization.json.JsonObject? = null,
        revenueChart: kotlinx.serialization.json.JsonArray = buildJsonArray {
            addChartPoint("2026-05-01", firstChartValue)
            addChartPoint("2026-05-02", 18.0)
        },
        orderChart: kotlinx.serialization.json.JsonArray? = buildJsonArray {
            addChartPoint("2026-05-01", 1.0)
            addChartPoint("2026-05-02", 3.0)
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
                    totals ?: buildJsonObject {
                        putAny("net_revenue", revenueTotal)
                        putAny("orders_count", orderCount)
                    }
                )
            }
            put("revenue_chart", revenueChart)
            orderChart?.let { put("order_chart", it) }
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
            is kotlinx.serialization.json.JsonElement -> put(name, value)
            else -> error("Unsupported test value $value")
        }
    }
}
