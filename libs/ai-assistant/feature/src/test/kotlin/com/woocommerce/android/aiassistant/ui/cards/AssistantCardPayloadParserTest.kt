package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantCardPayloadParserTest {
    @Test
    fun `given order payload, when parsed, then order card contains displayed fields`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(
                        family = "order",
                        id = "123",
                        title = "#1001",
                        details = ShowCardDetails.Order(
                            status = "processing",
                            total = "12.34",
                            currency = "USD",
                            dateCreated = "2026-05-01T10:00:00Z",
                            customerName = "Jane Doe",
                        ),
                    )
                )
            )
        )

        assertThat(cards).containsExactly(
            AssistantCard.Order(
                remoteOrderId = 123L,
                number = "#1001",
                status = "processing",
                total = "12.34",
                currency = "USD",
                customerName = "Jane Doe",
                date = "2026-05-01T10:00:00Z",
            )
        )
    }

    @Test
    fun `given order payload without status detail, when parsed, then status is empty`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(
                        family = "order",
                        id = "123",
                        title = "#1001",
                        details = ShowCardDetails.Order(total = "12.34"),
                    )
                )
            )
        )

        assertThat(cards.single()).isEqualTo(
            AssistantCard.Order(
                remoteOrderId = 123L,
                number = "#1001",
                status = "",
                total = "12.34",
                currency = "",
                customerName = "",
                date = "",
            )
        )
    }

    @Test
    fun `given order payload, when parsed as entries, then raw family id key is preserved`() {
        val entries = AssistantCardPayloadParser.parseEntries(
            ShowCardsUiStructured(
                cards = listOf(orderPayload(id = "00123", title = "#123"))
            )
        )

        assertThat(entries.single().key).isEqualTo(AssistantCardKey(family = "order", id = "00123"))
        assertThat(entries.single().card).isEqualTo(
            AssistantCard.Order(
                remoteOrderId = 123L,
                number = "#123",
                status = "processing",
                total = "",
                currency = "",
                customerName = "",
                date = "",
            )
        )
    }

    @Test
    fun `given product payload, when parsed, then product card contains displayed fields`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(
                        family = "product",
                        id = "456",
                        title = "Socks",
                        details = ShowCardDetails.Product(
                            sku = "woo-socks",
                            price = "9.99",
                            stockStatus = "instock",
                            status = "publish",
                            imageUrl = "https://example.com/socks.png",
                        ),
                    )
                )
            )
        )

        assertThat(cards).containsExactly(
            AssistantCard.Product(
                remoteProductId = 456L,
                name = "Socks",
                sku = "woo-socks",
                price = "9.99",
                stockStatus = "instock",
                status = "publish",
                imageUrl = "https://example.com/socks.png",
            )
        )
    }

    @Test
    fun `given customer payload, when parsed, then customer card contains displayed fields`() {
        val cards = AssistantCardPayloadParser.parse(
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

        assertThat(cards).containsExactly(
            AssistantCard.Customer(
                remoteCustomerId = 789L,
                name = "Ada Lovelace",
                email = "ada@example.com",
            )
        )
    }

    @Test
    fun `given unsupported and invalid payloads, when parsed, then they are ignored`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(
                        family = "coupon",
                        id = "456",
                        title = "Customer",
                        details = ShowCardDetails.Product(),
                    ),
                    ShowCardPayload(
                        family = "product",
                        id = "not-a-number",
                        title = "Bad product",
                        details = ShowCardDetails.Product(),
                    ),
                    ShowCardPayload(
                        family = "order",
                        id = "0",
                        title = "#0",
                        details = ShowCardDetails.Order(),
                    ),
                    ShowCardPayload(
                        family = "customer",
                        id = "0",
                        title = "Bad customer",
                        details = ShowCardDetails.Customer(),
                    ),
                )
            )
        )

        assertThat(cards).isEmpty()
    }

    @Test
    fun `given multiple order payloads, when parsed, then input order is preserved`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    orderPayload(id = "1", title = "#1"),
                    orderPayload(id = "2", title = "#2"),
                )
            )
        )

        assertThat(cards.map { (it as AssistantCard.Order).remoteOrderId }).containsExactly(1L, 2L)
    }

    @Test
    fun `given analytics stats payload, when parsed, then total and net sales card contains chart points`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(analyticsStatsPayload())
            )
        )

        assertThat(cards).containsExactly(
            AssistantCard.Stats(
                id = ANALYTICS_STATS_ID,
                kind = AssistantCard.Stats.Kind.Revenue,
                after = "2026-05-01",
                before = "2026-05-07",
                currency = "USD",
                metrics = listOf(
                    AssistantCard.Stats.Metric(
                        type = AssistantCard.Stats.MetricType.TotalSales,
                        value = "170.35",
                        chartPoints = listOf(
                            AssistantCard.Stats.ChartPoint("2026-05-01", 50.0),
                            AssistantCard.Stats.ChartPoint("2026-05-02", 120.35),
                        ),
                    ),
                    AssistantCard.Stats.Metric(
                        type = AssistantCard.Stats.MetricType.NetSales,
                        value = "120.15",
                        chartPoints = listOf(
                            AssistantCard.Stats.ChartPoint("2026-05-01", 35.0),
                            AssistantCard.Stats.ChartPoint("2026-05-02", 85.15),
                        ),
                    ),
                ),
            )
        )
    }

    @Test
    fun `given analytics stats payload with null total sales, when parsed, then gross sales fallback is used`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    analyticsStatsPayload(
                        totals = buildJsonObject {
                            put("total_sales", JsonNull)
                            put("gross_sales", "190.00")
                            put("net_revenue", "120.15")
                        },
                        intervalSubtotals = listOf(
                            analyticsInterval(
                                interval = "2026-05-01",
                                totalSales = JsonNull,
                                grossSales = "90.00",
                                netRevenue = "70.00",
                            )
                        ),
                    )
                )
            )
        )

        val statsCard = cards.single() as AssistantCard.Stats
        val totalSales = statsCard.metric(AssistantCard.Stats.MetricType.TotalSales)
        assertThat(totalSales.value).isEqualTo("190.00")
        assertThat(totalSales.chartPoints)
            .containsExactly(AssistantCard.Stats.ChartPoint("2026-05-01", 90.0))
    }

    @Test
    fun `given analytics stats payload with weekly interval label, when parsed, then date start fallback is used`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    analyticsStatsPayload(
                        intervalSubtotals = listOf(
                            analyticsInterval(
                                interval = "week-2026-18",
                                dateStart = "2026-05-04 00:00:00",
                                totalSales = "75.00",
                                netRevenue = "60.00",
                            )
                        ),
                    )
                )
            )
        )

        val statsCard = cards.single() as AssistantCard.Stats
        assertThat(statsCard.metric(AssistantCard.Stats.MetricType.TotalSales).chartPoints)
            .containsExactly(AssistantCard.Stats.ChartPoint("2026-05-04", 75.0))
        assertThat(statsCard.metric(AssistantCard.Stats.MetricType.NetSales).chartPoints)
            .containsExactly(AssistantCard.Stats.ChartPoint("2026-05-04", 60.0))
    }

    @Test
    fun `given analytics stats payload with malformed interval values, when parsed, then points are skipped per series`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    analyticsStatsPayload(
                        intervalSubtotals = listOf(
                            analyticsInterval(interval = "2026-05-01", totalSales = "n/a", netRevenue = "70.00"),
                            analyticsInterval(interval = "2026-05-02", totalSales = "90.00", netRevenue = "n/a"),
                            analyticsInterval(interval = "not-a-date", dateStart = "bad", totalSales = "1.00"),
                        ),
                    )
                )
            )
        )

        val statsCard = cards.single() as AssistantCard.Stats
        assertThat(statsCard.metric(AssistantCard.Stats.MetricType.TotalSales).chartPoints)
            .containsExactly(AssistantCard.Stats.ChartPoint("2026-05-02", 90.0))
        assertThat(statsCard.metric(AssistantCard.Stats.MetricType.NetSales).chartPoints)
            .containsExactly(AssistantCard.Stats.ChartPoint("2026-05-01", 70.0))
    }

    @Test
    fun `given analytics stats payload with invalid date range, when parsed, then stats card is ignored`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(analyticsStatsPayload(after = "bad-date"))
            )
        )

        assertThat(cards).isEmpty()
    }

    private fun orderPayload(id: String, title: String) = ShowCardPayload(
        family = "order",
        id = id,
        title = title,
        details = ShowCardDetails.Order(status = "processing"),
    )

    private fun analyticsStatsPayload(
        id: String = ANALYTICS_STATS_ID,
        after: String = "2026-05-01",
        before: String = "2026-05-07",
        totals: JsonObject = buildJsonObject {
            put("total_sales", "170.35")
            put("gross_sales", "190.00")
            put("net_revenue", "120.15")
        },
        intervalSubtotals: List<JsonObject> = listOf(
            analyticsInterval("2026-05-01", totalSales = "50.00", netRevenue = "35.00"),
            analyticsInterval("2026-05-02", totalSales = "120.35", netRevenue = "85.15"),
        ),
    ) = ShowCardPayload(
        family = "analytics_stats",
        id = id,
        title = "Analytics",
        details = ShowCardDetails.AnalyticsStats(
            after = after,
            before = before,
            currency = "USD",
            totals = totals,
            intervalSubtotals = intervalSubtotals,
        ),
    )

    private fun analyticsInterval(
        interval: String,
        totalSales: Any? = null,
        grossSales: Any? = null,
        netRevenue: Any? = null,
        dateStart: String? = "$interval 00:00:00",
    ) = buildJsonObject {
        put("interval", interval)
        dateStart?.let { put("date_start", it) }
        putJsonObject("subtotals") {
            totalSales?.let { putAny("total_sales", it) }
            grossSales?.let { putAny("gross_sales", it) }
            netRevenue?.let { putAny("net_revenue", it) }
        }
    }

    private fun JsonObjectBuilder.putAny(name: String, value: Any) {
        when (value) {
            is Number -> put(name, value)
            is String -> put(name, value)
            is JsonElement -> put(name, value)
            else -> error("Unsupported test value $value")
        }
    }

    private fun AssistantCard.Stats.metric(type: AssistantCard.Stats.MetricType): AssistantCard.Stats.Metric =
        metrics.single { it.type == type }

    private companion object {
        private const val ANALYTICS_STATS_ID =
            "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:USD"
    }
}
