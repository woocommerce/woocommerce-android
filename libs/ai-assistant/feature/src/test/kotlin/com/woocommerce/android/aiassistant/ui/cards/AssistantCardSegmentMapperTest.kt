package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import com.woocommerce.android.aiassistant.tools.products.CompactVariationAttribute
import com.woocommerce.android.aiassistant.ui.AssistantUiSegment
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantCardSegmentMapperTest {
    @Test
    fun `given order and product payloads, when mapped, then grouped segment preserves parsed card order`() {
        val segments = AssistantCardSegmentMapper.toSegments(
            ShowCardsUiStructured(
                cards = listOf(
                    orderPayload(id = "1", title = "#1"),
                    productPayload(id = "2", title = "Socks"),
                )
            )
        )

        assertThat(segments).containsExactly(
            AssistantUiSegment.CardGroup(
                listOf(
                    AssistantCard.Order(
                        remoteOrderId = 1L,
                        number = "#1",
                        status = "processing",
                        total = "12.34",
                        currency = "USD",
                        customerName = "Jane Doe",
                        date = "2026-05-01T10:00:00Z",
                    ),
                    AssistantCard.Product(
                        remoteProductId = 2L,
                        name = "Socks",
                        sku = "woo-socks",
                        price = "9.99",
                        stockStatus = "instock",
                        status = "publish",
                        imageUrl = "https://example.com/socks.png",
                    ),
                )
            )
        )
    }

    @Test
    fun `given unsupported payload, when mapped, then no segments are returned`() {
        val segments = AssistantCardSegmentMapper.toSegments(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(
                        family = "customer",
                        id = "456",
                        title = "Customer",
                        details = ShowCardDetails.Product(),
                    ),
                    ShowCardPayload(
                        family = "order",
                        id = "not-a-number",
                        title = "#bad",
                        details = ShowCardDetails.Order(),
                    ),
                )
            )
        )

        assertThat(segments).isEmpty()
    }

    @Test
    fun `given analytics stats payload, when mapped, then grouped segment includes stats card`() {
        val segments = AssistantCardSegmentMapper.toSegments(
            ShowCardsUiStructured(
                cards = listOf(analyticsStatsPayload())
            )
        )

        assertThat(segments).containsExactly(
            AssistantUiSegment.CardGroup(
                listOf(
                    AssistantCard.Stats(
                        id = ANALYTICS_STATS_ID,
                        after = "2026-05-01",
                        before = "2026-05-07",
                        currency = "USD",
                        metrics = listOf(
                            AssistantCard.Stats.Metric(
                                type = AssistantCard.Stats.MetricType.TotalSales,
                                value = "170.35",
                                chartPoints = listOf(AssistantCard.Stats.ChartPoint("2026-05-01", 170.35)),
                            ),
                            AssistantCard.Stats.Metric(
                                type = AssistantCard.Stats.MetricType.NetSales,
                                value = "120.15",
                                chartPoints = listOf(AssistantCard.Stats.ChartPoint("2026-05-01", 120.15)),
                            ),
                            AssistantCard.Stats.Metric(
                                type = AssistantCard.Stats.MetricType.TotalOrders,
                                value = "42",
                                chartPoints = listOf(AssistantCard.Stats.ChartPoint("2026-05-01", 42.0)),
                            ),
                            AssistantCard.Stats.Metric(
                                type = AssistantCard.Stats.MetricType.AverageOrderValue,
                                value = "85.30",
                                chartPoints = listOf(AssistantCard.Stats.ChartPoint("2026-05-01", 85.30)),
                            ),
                        ),
                    )
                )
            )
        )
    }

    @Test
    fun `given variation payload, when mapped, then grouped segment contains typed variation card`() {
        val segments = AssistantCardSegmentMapper.toSegments(
            ShowCardsUiStructured(
                cards = listOf(variationPayload())
            )
        )

        assertThat(segments).containsExactly(
            AssistantUiSegment.CardGroup(
                listOf(
                    AssistantCard.Variation(
                        parentProductId = 100L,
                        variationId = 10L,
                        parentProductName = "Woo socks",
                        sku = "woo-socks-blue",
                        price = "12.99",
                        stockStatus = "instock",
                        status = "publish",
                        imageUrl = "https://example.com/blue-socks.png",
                        attributes = listOf(
                            AssistantCard.Variation.Attribute(name = "Size", option = "M"),
                            AssistantCard.Variation.Attribute(name = "Color", option = "Blue"),
                        ),
                    )
                )
            )
        )
    }

    @Test
    fun `given multiple variation payloads, when mapped, then grouped segment preserves variation order`() {
        val segments = AssistantCardSegmentMapper.toSegments(
            ShowCardsUiStructured(
                cards = listOf(
                    variationPayload(id = "100/10", productId = 100L, variationId = 10L),
                    variationPayload(id = "100/11", productId = 100L, variationId = 11L),
                    variationPayload(id = "101/10", productId = 101L, variationId = 10L),
                )
            )
        )

        val cardGroup = segments.single() as AssistantUiSegment.CardGroup
        val variations = cardGroup.cards.filterIsInstance<AssistantCard.Variation>()
        assertThat(variations.map { "${it.parentProductId}/${it.variationId}" })
            .containsExactly("100/10", "100/11", "101/10")
        assertThat(variations.map { it.parentProductName }).containsExactly("Woo socks", "Woo socks", "Woo socks")
    }

    private fun orderPayload(id: String, title: String) = ShowCardPayload(
        family = "order",
        id = id,
        title = title,
        details = ShowCardDetails.Order(
            status = "processing",
            total = "12.34",
            currency = "USD",
            dateCreated = "2026-05-01T10:00:00Z",
            customerName = "Jane Doe",
        ),
    )

    private fun productPayload(id: String, title: String) = ShowCardPayload(
        family = "product",
        id = id,
        title = title,
        details = ShowCardDetails.Product(
            sku = "woo-socks",
            price = "9.99",
            stockStatus = "instock",
            status = "publish",
            imageUrl = "https://example.com/socks.png",
        ),
    )

    private fun variationPayload(
        id: String = "100/10",
        productId: Long = 100L,
        variationId: Long = 10L,
    ) = ShowCardPayload(
        family = "variation",
        id = id,
        title = "Size: M \u2022 Color: Blue",
        details = ShowCardDetails.Variation(
            productId = productId,
            variationId = variationId,
            parentProductName = "Woo socks",
            sku = "woo-socks-blue",
            price = "12.99",
            stockStatus = "instock",
            status = "publish",
            imageUrl = "https://example.com/blue-socks.png",
            attributes = listOf(
                CompactVariationAttribute(name = "Size", option = "M"),
                CompactVariationAttribute(name = "Color", option = "Blue"),
            ),
        ),
    )

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
                put("orders_count", "42")
                put("avg_order_value", "85.30")
            },
            intervalSubtotals = listOf(
                buildJsonObject {
                    put("interval", "2026-05-01")
                    putJsonObject("subtotals") {
                        put("total_sales", "170.35")
                        put("net_revenue", "120.15")
                        put("orders_count", "42")
                        put("avg_order_value", "85.30")
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
