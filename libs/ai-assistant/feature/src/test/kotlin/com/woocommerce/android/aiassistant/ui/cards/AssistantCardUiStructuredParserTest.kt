package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantCardUiStructuredParserTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }
    private val parser = AssistantCardUiStructuredParser(json)

    @Test
    fun `given missing uiStructured, when parsed, then no cards are returned`() {
        assertThat(parser.parse(null)).isEmpty()
    }

    @Test
    fun `given malformed cards field, when parsed, then no cards are returned`() {
        val cards = parser.parse(
            buildJsonObject {
                put("cards", "not an array")
            }
        )

        assertThat(cards).isEmpty()
    }

    @Test
    fun `given malformed card details, when parsed, then no cards are returned`() {
        val cards = parser.parse(
            buildJsonObject {
                putJsonArray("cards") {
                    add(
                        buildJsonObject {
                            put("family", "order")
                            put("id", "123")
                            put("title", "#123")
                            put("details", "not an object")
                        }
                    )
                }
            }
        )

        assertThat(cards).isEmpty()
    }

    @Test
    fun `given valid uiStructured, when parsed, then card entries are returned`() {
        val cards = parser.parse(
            json.encodeToJsonElement(
                ShowCardsUiStructured(
                    cards = listOf(
                        ShowCardPayload(
                            family = "order",
                            id = "123",
                            title = "#123",
                            details = ShowCardDetails.Order(status = "processing"),
                        )
                    )
                )
            )
        )

        assertThat(cards.map { it.key }).containsExactly(AssistantCardKey("order", "123"))
    }

    @Test
    fun `given customer uiStructured, when parsed, then customer card entry is returned`() {
        val entries = parser.parse(
            json.encodeToJsonElement(
                ShowCardsUiStructured(
                    cards = listOf(
                        ShowCardPayload(
                            family = "customer",
                            id = "789",
                            title = "Ada Lovelace",
                            details = ShowCardDetails.Customer(email = "ada@example.com"),
                        )
                    )
                )
            )
        )

        assertThat(entries.single().key).isEqualTo(AssistantCardKey("customer", "789"))
        assertThat(entries.single().card).isEqualTo(
            AssistantCard.Customer(
                remoteCustomerId = 789L,
                name = "Ada Lovelace",
                email = "ada@example.com",
            )
        )
    }

    @Test
    fun `given analytics stats uiStructured, when parsed, then stats card entry is returned`() {
        val entries = parser.parse(
            json.encodeToJsonElement(
                ShowCardsUiStructured(
                    cards = listOf(analyticsStatsPayload())
                )
            )
        )

        assertThat(entries.single().key).isEqualTo(AssistantCardKey("analytics_stats", ANALYTICS_STATS_ID))
        val card = entries.single().card as AssistantCard.Stats
        assertThat(card.metric(AssistantCard.Stats.MetricType.TotalSales).value).isEqualTo("170.35")
        assertThat(card.metric(AssistantCard.Stats.MetricType.NetSales).chartPoints)
            .containsExactly(AssistantCard.Stats.ChartPoint("2026-05-01", 120.15))
    }

    @Test
    fun `given orders analytics stats uiStructured, when parsed, then order metric stats card entry is returned`() {
        val entries = parser.parse(
            json.encodeToJsonElement(
                ShowCardsUiStructured(
                    cards = listOf(ordersAnalyticsStatsPayload())
                )
            )
        )

        assertThat(entries.single().key).isEqualTo(AssistantCardKey("analytics_stats", ANALYTICS_STATS_ID))
        val card = entries.single().card as AssistantCard.Stats
        assertThat(card.metric(AssistantCard.Stats.MetricType.TotalOrders).value).isEqualTo("42")
        assertThat(card.metric(AssistantCard.Stats.MetricType.AverageOrderValue).chartPoints)
            .containsExactly(AssistantCard.Stats.ChartPoint("2026-05-01", 80.10))
    }

    private fun analyticsStatsPayload() = ShowCardPayload(
        family = "analytics_stats",
        id = ANALYTICS_STATS_ID,
        title = "Analytics",
        details = ShowCardDetails.AnalyticsStats(
            after = "2026-05-01",
            before = "2026-05-07",
            currency = "USD",
            totals = buildJsonObject {
                put("total_sales", "170.35")
                put("net_revenue", "120.15")
            },
            intervalSubtotals = listOf(
                buildJsonObject {
                    put("interval", "2026-05-01")
                    putJsonObject("subtotals") {
                        put("total_sales", "170.35")
                        put("net_revenue", "120.15")
                    }
                }
            ),
        ),
    )

    private fun ordersAnalyticsStatsPayload() = ShowCardPayload(
        family = "analytics_stats",
        id = ANALYTICS_STATS_ID,
        title = "Analytics",
        details = ShowCardDetails.AnalyticsStats(
            after = "2026-05-01",
            before = "2026-05-07",
            currency = "USD",
            totals = buildJsonObject {
                put("orders_count", "42")
                put("avg_order_value", "85.30")
            },
            intervalSubtotals = listOf(
                buildJsonObject {
                    put("interval", "2026-05-01")
                    putJsonObject("subtotals") {
                        put("orders_count", "12")
                        put("avg_order_value", "80.10")
                    }
                }
            ),
        ),
    )

    private companion object {
        private const val ANALYTICS_STATS_ID =
            "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day"
    }
}
