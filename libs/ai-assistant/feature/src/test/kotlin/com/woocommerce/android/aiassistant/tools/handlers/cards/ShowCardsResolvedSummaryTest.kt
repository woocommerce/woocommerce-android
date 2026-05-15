package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.tools.orders.CompactOrderLineItem
import com.woocommerce.android.aiassistant.tools.products.CompactVariationAttribute
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ShowCardsResolvedSummaryTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    @Test
    fun `given typed summaries, when serialized, then each family keeps its expected summary keys`() {
        val summaries = listOf(
            orderSummaryWithKeys(),
            productSummaryWithKeys(),
            variationSummaryWithKeys(),
            analyticsStatsSummaryWithKeys(),
            customerSummaryWithKeys(),
        )

        summaries.forEach { (summary, expectedKeys) ->
            assertThat(summary.toJsonObject(json).keys).containsExactlyElementsOf(expectedKeys)
        }
    }

    @Test
    fun `given analytics stats summary, when serialized, then nested metric objects are preserved`() {
        val totals = buildJsonObject {
            put("total_sales", "170.35")
            put("net_revenue", "120.15")
        }
        val interval = buildJsonObject {
            put("interval", "2026-05-01")
            put("date_start", "2026-05-01 00:00:00")
        }

        val summary = ShowCardsResolvedSummary.AnalyticsStats(
            AnalyticsStatsSummary(
                id = "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day",
                after = "2026-05-01",
                before = "2026-05-07",
                currency = "USD",
                totals = totals,
                intervalSubtotals = listOf(interval),
            )
        ).toJsonObject(json)

        assertThat(summary.getValue("totals").jsonObject.getValue("net_revenue").jsonPrimitive.content)
            .isEqualTo("120.15")
        assertThat(summary.getValue("interval_subtotals").jsonArray).hasSize(1)
    }

    private fun orderSummaryWithKeys() = ShowCardsResolvedSummary.Order(
        OrderSummary(
            id = "123",
            number = "#123",
            status = "processing",
            total = "12.34",
            currency = "USD",
            dateCreated = "2026-05-01T10:00:00Z",
            customerName = "Jane Doe",
            paymentMethodTitle = "Credit Card",
            customerId = 55L,
            lineItemsCount = 1,
            lineItems = listOf(CompactOrderLineItem(id = 10L, name = "Socks", quantity = 1f)),
        )
    ) to listOf(
        "id",
        "number",
        "status",
        "total",
        "currency",
        "date_created",
        "customer_name",
        "payment_method_title",
        "customer_id",
        "line_items_count",
        "line_items",
    )

    private fun productSummaryWithKeys() = ShowCardsResolvedSummary.Product(
        ProductSummary(
            id = "456",
            name = "Socks",
            sku = "woo-socks",
            price = "9.99",
            type = "simple",
            stockStatus = "instock",
            manageStock = true,
            onSale = false,
            stockQuantity = 12.0,
        )
    ) to listOf(
        "id",
        "name",
        "sku",
        "price",
        "type",
        "stock_status",
        "manage_stock",
        "on_sale",
        "stock_quantity",
    )

    private fun variationSummaryWithKeys() = ShowCardsResolvedSummary.Variation(
        VariationSummary(
            id = "100/10",
            productId = 100L,
            variationId = 10L,
            sku = "woo-socks-blue",
            price = "12.99",
            stockStatus = "instock",
            status = "publish",
            attributes = listOf(CompactVariationAttribute(name = "Size", option = "M")),
        )
    ) to listOf(
        "id",
        "product_id",
        "variation_id",
        "sku",
        "price",
        "stock_status",
        "status",
        "attributes",
    )

    private fun analyticsStatsSummaryWithKeys() = ShowCardsResolvedSummary.AnalyticsStats(
        AnalyticsStatsSummary(
            id = "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day",
            after = "2026-05-01",
            before = "2026-05-07",
            currency = "USD",
            totals = buildJsonObject { put("total_sales", "170.35") },
            intervalSubtotals = listOf(buildJsonObject { put("interval", "2026-05-01") }),
        )
    ) to listOf(
        "id",
        "after",
        "before",
        "currency",
        "totals",
        "interval_subtotals",
    )

    private fun customerSummaryWithKeys() = ShowCardsResolvedSummary.Customer(
        CustomerSummary(
            id = "789",
            name = "Ada Lovelace",
            email = "ada@example.com",
        )
    ) to listOf("id", "name", "email")
}
