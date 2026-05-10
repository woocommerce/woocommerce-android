package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.tools.handlers.cards.CustomerSummary
import com.woocommerce.android.aiassistant.tools.handlers.cards.OrderSummary
import com.woocommerce.android.aiassistant.tools.handlers.cards.ProductSummary
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardFamily
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsRejectionReason
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolution
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolver
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import com.woocommerce.android.aiassistant.tools.handlers.cards.ValidatedRef
import com.woocommerce.android.aiassistant.tools.handlers.cards.VariationSummary
import com.woocommerce.android.aiassistant.tools.orders.CompactOrderLineItem
import com.woocommerce.android.aiassistant.tools.products.CompactVariationAttribute
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ShowCardsToolHandlerTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }
    private val handler = handlerWith(FakeResolver.empty())

    @Test
    fun `when descriptor is inspected, then show cards accepts order product variation analytics stats and customer references`() {
        val descriptor = handler.descriptor

        assertThat(descriptor.name).isEqualTo("show_cards")
        assertThat(descriptor.description).contains("order")
        assertThat(descriptor.description).contains("product")
        assertThat(descriptor.description).contains("variation")
        assertThat(descriptor.description).contains("analytics_stats")
        assertThat(descriptor.description).contains("customer")
        assertThat(descriptor.inputSchema.toString()).contains("references")
        assertThat(descriptor.inputSchema.toString()).contains("family")
        assertThat(descriptor.inputSchema.toString()).contains("id")
        assertThat(descriptor.inputSchema.toString()).contains("order")
        assertThat(descriptor.inputSchema.toString()).contains("product")
        assertThat(descriptor.inputSchema.toString()).contains("variation")
        assertThat(descriptor.inputSchema.toString()).contains("analytics_stats")
        assertThat(descriptor.inputSchema.toString()).contains("customer")
        assertThat(descriptor.inputSchema.toString()).contains("{parentProductId}/{variationId}")
        assertThat(descriptor.inputSchema.toString())
            .contains("analytics_orders:after:<YYYY-MM-DD>:before:<YYYY-MM-DD>")
        assertThat(descriptor.description).doesNotContain("analytics_revenue")
        assertThat(descriptor.inputSchema.toString()).doesNotContain("analytics_revenue")
        assertThat(descriptor.inputSchema.toString()).contains("card_id")
        assertThat(descriptor.inputSchema.toString()).doesNotContain("\"totals\"")
        assertThat(descriptor.inputSchema.toString()).doesNotContain("\"interval_subtotals\"")
        assertThat(descriptor.inputSchema.toString()).doesNotContain("\"cards\"")
    }

    @Test
    fun `when descriptor is inspected, then show_cards is safe`() {
        assertThat(handler.descriptor.safetyLevel).isEqualTo(ToolSafetyLevel.SAFE)
    }

    @Test
    fun `given resolved ref, when executed, then structured contains compact result counts and ref lists`() = runTest {
        val result = executeShowCards(
            handler = handlerWith(FakeResolver.resolving(orderCard(id = "123"))),
            argumentsJson = """{"references":[{"family":"order","id":"123"}]}"""
        )

        val structured = assertSuccess(result).structured.jsonObject

        assertThat(structured.keys).containsExactly(
            "requested",
            "validated",
            "rendered",
            "resolved_refs",
            "missing_refs",
            "rejected_refs"
        )
        assertThat(structured["requested"]?.jsonPrimitive?.int).isEqualTo(1)
        assertThat(structured["validated"]?.jsonPrimitive?.int).isEqualTo(1)
        assertThat(structured["rendered"]?.jsonPrimitive?.int).isEqualTo(1)
    }

    @Test
    fun `given resolved order, when executed, then summary contains only allowlisted fields`() = runTest {
        val result = callShowCards(FakeResolver.resolving(orderCard(id = "123")))

        val summary = firstResolvedSummary(result)

        assertThat(summary.keys).containsExactly(
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
    }

    @Test
    fun `given resolved product, when executed, then summary contains only allowlisted fields`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(productCard(id = "456")),
            referencesJson = """[{ "family": "product", "id": "456" }]"""
        )

        val summary = firstResolvedSummary(result)

        assertThat(summary.keys).containsExactly(
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
    }

    @Test
    fun `given resolved analytics stats, when executed, then summary contains only allowlisted fields`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(analyticsStatsCard(id = ANALYTICS_STATS_ID)),
            referencesJson = """[{ "family": "analytics_stats", "id": "$ANALYTICS_STATS_ID" }]"""
        )

        val summary = firstResolvedSummary(result)

        assertThat(summary.keys).containsExactly(
            "id",
            "after",
            "before",
            "currency",
            "totals",
            "interval_subtotals",
        )
    }

    @Test
    fun `given resolved customer, when executed, then summary contains only allowlisted fields`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(customerCard(id = "123")),
            referencesJson = """[{ "family": "customer", "id": "123" }]"""
        )

        val summary = firstResolvedSummary(result)

        assertThat(summary.keys).containsExactly("id", "name", "email")
    }

    @Test
    fun `given resolved variation, when executed, then summary contains only allowlisted fields`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(variationCard(id = "100/10")),
            referencesJson = """[{ "family": "variation", "id": "100/10" }]"""
        )

        val summary = firstResolvedSummary(result)

        assertThat(summary.keys).containsExactly(
            "id",
            "product_id",
            "variation_id",
            "name",
            "sku",
            "price",
            "stock_status",
            "status",
            "attributes",
        )
        assertThat(assertSuccess(result).structured.toString()).doesNotContain("image_url")
    }

    @Test
    fun `given analytics stats id, when executed, then ref validates and resolves`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(
                analyticsStatsCard(id = ANALYTICS_STATS_ID),
            ),
            referencesJson = """
                [
                  { "family": "analytics_stats", "id": "$ANALYTICS_STATS_ID" }
                ]
            """.trimIndent()
        )

        assertThat(validated(result)).isEqualTo(1)
        assertThat(rejectedReasons(result)).isEmpty()
        assertThat(resolvedIds(result)).containsExactly(ANALYTICS_STATS_ID)
    }

    @Test
    fun `given variation composite id, when executed, then ref validates and resolved id is preserved`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(variationCard(id = "100/10")),
            referencesJson = """[{ "family": "variation", "id": "100/10" }]"""
        )

        assertThat(validated(result)).isEqualTo(1)
        assertThat(rejectedReasons(result)).isEmpty()
        assertThat(resolvedFamilies(result)).containsExactly("variation")
        assertThat(resolvedIds(result)).containsExactly("100/10")
    }

    @Test
    fun `given invalid variation composite ids, when executed, then refs are rejected as invalid id`() = runTest {
        val refs = listOf("100", "100/", "/10", "0/10", "100/0", "abc/10", "100/abc", "100:10")
            .joinToString(separator = ",") { id -> """{ "family": "variation", "id": "$id" }""" }

        val result = callShowCards(
            resolver = FakeResolver.empty(),
            referencesJson = "[$refs]"
        )

        assertThat(validated(result)).isEqualTo(0)
        assertThat(rejectedReasons(result)).containsExactly(
            "invalid_id",
            "invalid_id",
            "invalid_id",
            "invalid_id",
            "invalid_id",
            "invalid_id",
            "invalid_id",
            "invalid_id",
        )
    }

    @Test
    fun `given resolver returns summary extras, when executed, then structured excludes private fields`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(leakyProductCard(id = "456")),
            referencesJson = """[{ "family": "product", "id": "456" }]"""
        )

        val structuredText = assertSuccess(result).structured.toString()

        assertThat(structuredText).doesNotContain("Long description")
        assertThat(structuredText).doesNotContain("<p>Private</p>")
        assertThat(structuredText).doesNotContain("image.png")
        assertThat(structuredText).doesNotContain("metadata")
        assertThat(structuredText).doesNotContain("raw")
    }

    @Test
    fun `given resolved analytics stats extras, when executed, then structured excludes private fields`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(leakyAnalyticsStatsCard(id = ANALYTICS_STATS_ID)),
            referencesJson = """[{ "family": "analytics_stats", "id": "$ANALYTICS_STATS_ID" }]"""
        )

        val structuredText = assertSuccess(result).structured.toString()

        assertThat(structuredText).contains("interval_subtotals")
        assertThat(structuredText).doesNotContain("private_total")
        assertThat(structuredText).doesNotContain("debug")
    }

    @Test
    fun `given resolved ref, when executed, then uiStructured contains cards`() = runTest {
        val result = callShowCards(FakeResolver.resolving(orderCard(id = "123")))

        val uiStructured = requireNotNull(assertSuccess(result).uiStructured).jsonObject
        val cards = uiStructured.getValue("cards").jsonArray

        assertThat(cards).hasSize(1)
        assertThat(cards.first().jsonObject["family"]?.jsonPrimitive?.content).isEqualTo("order")
        assertThat(cards.first().jsonObject["id"]?.jsonPrimitive?.content).isEqualTo("123")
    }

    @Test
    fun `given resolved order, when executed, then uiStructured contains typed order details`() = runTest {
        val result = callShowCards(FakeResolver.resolving(orderCard(id = "123")))

        val card = typedUiCards(result).single()
        val details = card.details as ShowCardDetails.Order

        assertThat(card.family).isEqualTo("order")
        assertThat(card.id).isEqualTo("123")
        assertThat(card.title).isEqualTo("#123")
        assertThat(details.status).isEqualTo("processing")
        assertThat(details.total).isEqualTo("12.34")
        assertThat(details.currency).isEqualTo("USD")
        assertThat(details.dateCreated).isEqualTo("2026-05-01T10:00:00Z")
        assertThat(details.customerName).isEqualTo("Jane Doe")
        assertThat(uiCards(result).single().jsonObject.keys).doesNotContain("subtitle", "badges", "attributes")
        assertThat(assertSuccess(result).structured.toString()).doesNotContain("details")
    }

    @Test
    fun `given resolved product, when executed, then uiStructured contains typed product details`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(productCard(id = "456")),
            referencesJson = """[{ "family": "product", "id": "456" }]"""
        )

        val card = typedUiCards(result).single()
        val details = card.details as ShowCardDetails.Product

        assertThat(card.family).isEqualTo("product")
        assertThat(card.id).isEqualTo("456")
        assertThat(card.title).isEqualTo("Socks")
        assertThat(details.sku).isEqualTo("woo-socks")
        assertThat(details.price).isEqualTo("9.99")
        assertThat(details.stockStatus).isEqualTo("instock")
        assertThat(details.status).isEqualTo("publish")
        assertThat(details.imageUrl).isEqualTo(PRODUCT_IMAGE_URL)
        assertThat(uiCards(result).single().jsonObject.keys).doesNotContain("subtitle", "badges", "attributes")
        assertThat(assertSuccess(result).structured.toString()).doesNotContain("details")
        assertThat(assertSuccess(result).structured.toString()).doesNotContain("image_url")
    }

    @Test
    fun `given resolved analytics stats, when executed, then uiStructured contains typed analytics details`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(analyticsStatsCard(id = ANALYTICS_STATS_ID)),
            referencesJson = """[{ "family": "analytics_stats", "id": "$ANALYTICS_STATS_ID" }]"""
        )

        val card = typedUiCards(result).single()
        val details = card.details as ShowCardDetails.AnalyticsStats

        assertThat(card.family).isEqualTo("analytics_stats")
        assertThat(card.id).isEqualTo(ANALYTICS_STATS_ID)
        assertThat(card.title).isEqualTo("Analytics")
        assertThat(details.after).isEqualTo("2026-05-01")
        assertThat(details.before).isEqualTo("2026-05-07")
        assertThat(details.currency).isEqualTo("USD")
        assertThat(details.totals.getValue("total_sales").jsonPrimitive.content).isEqualTo("170.35")
        assertThat(details.intervalSubtotals).hasSize(1)
        assertThat(assertSuccess(result).structured.toString()).contains("interval_subtotals")
    }

    @Test
    fun `given invalid refs, when executed, then rejected refs use lower snake case reasons`() = runTest {
        val result = executeShowCards(
            handler = handler,
            argumentsJson = """
            {
              "references": [
                null,
                { "id": "1" },
                { "family": "coupon", "id": "1" },
                { "family": "order" },
                { "family": "product", "id": "" }
              ]
            }
            """.trimIndent()
        )

        val reasons = rejectedReasons(result)

        assertThat(reasons).containsExactly(
            "malformed_ref",
            "missing_family",
            "unsupported_family",
            "missing_id",
            "invalid_id"
        )
    }

    @Test
    fun `given non string present ids, when executed, then ids are invalid`() = runTest {
        val result = executeShowCards(
            handler = handler,
            argumentsJson = """
            {
              "references": [
                { "family": "order", "id": 123 },
                { "family": "product", "id": true },
                { "family": "order", "id": null },
                { "family": "analytics_stats", "id": 123 }
              ]
            }
            """.trimIndent()
        )

        assertThat(rejectedReasons(result)).containsExactly("invalid_id", "invalid_id", "invalid_id", "invalid_id")
    }

    @Test
    fun `given non numeric string ids, when executed, then ids are invalid and not resolved`() = runTest {
        val result = executeShowCards(
            handler = handlerWith(FakeResolver.empty()),
            argumentsJson = """
            {
              "references": [
                { "family": "order", "id": "abc" },
                { "family": "product", "id": "12x" },
                { "family": "customer", "id": "12x" },
                { "family": "order", "id": "0" },
                { "family": "customer", "id": "0" }
              ]
            }
            """.trimIndent()
        )

        assertThat(validated(result)).isEqualTo(0)
        assertThat(rejectedReasons(result))
            .containsExactly("invalid_id", "invalid_id", "invalid_id", "invalid_id", "invalid_id")
    }

    @Test
    fun `given malformed analytics stats id, when executed, then ref is rejected as invalid id`() = runTest {
        val result = executeShowCards(
            handler = handlerWith(FakeResolver.empty()),
            argumentsJson = """
            {
              "references": [
                { "family": "analytics_stats", "id": "analytics_revenue:2026-05-01:2026-05-07" },
                { "family": "analytics_stats", "id": "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:USD" },
                { "family": "analytics_stats", "id": "analytics_orders:after:2026-05-07:before:2026-05-01:interval:day" },
                { "family": "analytics_stats", "id": "analytics_orders:after:2026-05-01:before:2026-05-07:interval:bad" }
              ]
            }
            """.trimIndent()
        )

        assertThat(validated(result)).isEqualTo(0)
        assertThat(rejectedReasons(result)).containsExactly("invalid_id", "invalid_id", "invalid_id", "invalid_id")
    }

    @Test
    fun `given unknown analytics stats prefixes, when executed, then refs are rejected as invalid id`() = runTest {
        val result = executeShowCards(
            handler = handlerWith(FakeResolver.empty()),
            argumentsJson = """
            {
              "references": [
                { "family": "analytics_stats", "id": "analytics_products:after:2026-05-01:before:2026-05-07:interval:day" },
                { "family": "analytics_stats", "id": "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day:currency:none" }
              ]
            }
            """.trimIndent()
        )

        assertThat(validated(result)).isEqualTo(0)
        assertThat(rejectedReasons(result)).containsExactly("invalid_id", "invalid_id")
    }

    @Test
    fun `given duplicate refs, when executed, then duplicates after first family id pair are rejected`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(
                orderCard(id = "123"),
                productCard(id = "123"),
                analyticsStatsCard(id = ANALYTICS_STATS_ID),
            ),
            referencesJson = """
                [
                  { "family": "order", "id": "123" },
                  { "family": "order", "id": "123" },
                  { "family": "product", "id": "123" },
                  { "family": "analytics_stats", "id": "$ANALYTICS_STATS_ID" },
                  { "family": "analytics_stats", "id": "$ANALYTICS_STATS_ID" }
                ]
            """.trimIndent()
        )

        assertThat(validated(result)).isEqualTo(3)
        assertThat(rejectedReasons(result)).containsExactly("duplicate_ref", "duplicate_ref")
        assertThat(resolvedFamilies(result)).containsExactly("order", "product", "analytics_stats")
    }

    @Test
    fun `given more than ten refs, when executed, then refs after max ten are rejected and not rendered`() = runTest {
        val refs = (1..11).joinToString(separator = ",") { index ->
            """{ "family": "order", "id": "$index" }"""
        }

        val result = callShowCards(
            resolver = FakeResolver.resolving(*(1..10).map { orderCard(id = it.toString()) }.toTypedArray()),
            referencesJson = "[$refs]"
        )

        assertThat(validated(result)).isEqualTo(10)
        assertThat(rendered(result)).isEqualTo(10)
        assertThat(rejectedReasons(result).last()).isEqualTo("over_limit")
        assertThat(resolvedIds(result)).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
    }

    @Test
    fun `given resolver throws, when executed, then valid refs are missing with fetch failed`() = runTest {
        val result = executeShowCards(
            handler = handlerWith(ThrowingResolver),
            argumentsJson = """{"references":[{"family":"order","id":"123"}]}"""
        )

        assertThat(missingReasons(result)).containsExactly("fetch_failed")
        assertThat(rendered(result)).isEqualTo(0)
        assertThat(uiCards(result)).isEmpty()
    }

    @Test
    fun `given one resolved and one missing ref, when executed, then structured and ui cards preserve partial success`() =
        runTest {
            val result = callShowCards(
                resolver = FakeResolver.returning(
                    listOf(
                        orderCard(id = "123"),
                        ShowCardsResolution.Missing(
                            ref = ValidatedRef(index = 1, family = ShowCardFamily.Product, id = "456"),
                            reason = ShowCardsRejectionReason.NotFound,
                        ),
                    )
                ),
                referencesJson = """
                    [
                      { "family": "order", "id": "123" },
                      { "family": "product", "id": "456" }
                    ]
                """.trimIndent(),
            )

            assertThat(rendered(result)).isEqualTo(1)
            assertThat(missingReasons(result)).containsExactly("not_found")
            assertThat(uiCards(result)).hasSize(1)
        }

    private fun handlerWith(resolver: ShowCardsResolver) =
        ShowCardsToolHandler(resolver)

    private suspend fun executeShowCards(
        handler: ShowCardsToolHandler,
        argumentsJson: String,
    ): ToolResult = handler.execute(
        ToolCall(
            id = "call_1",
            name = "show_cards",
            arguments = json.parseToJsonElement(argumentsJson).jsonObject,
        )
    )

    private suspend fun callShowCards(
        resolver: FakeResolver,
        referencesJson: String = """[{ "family": "order", "id": "123" }]""",
    ): ToolResult = executeShowCards(
        handler = handlerWith(resolver),
        argumentsJson = """{ "references": $referencesJson }""",
    )

    private fun assertSuccess(result: ToolResult): ToolResult.Success {
        assertThat(result).isInstanceOf(ToolResult.Success::class.java)
        return result as ToolResult.Success
    }

    private fun firstResolvedSummary(result: ToolResult) =
        assertSuccess(result).structured.jsonObject
            .getValue("resolved_refs").jsonArray
            .first().jsonObject
            .getValue("summary").jsonObject

    private fun rejectedReasons(result: ToolResult): List<String> =
        assertSuccess(result).structured.jsonObject
            .getValue("rejected_refs").jsonArray
            .map { ref -> ref.jsonObject.getValue("reason").jsonPrimitive.content }

    private fun missingReasons(result: ToolResult): List<String> =
        assertSuccess(result).structured.jsonObject
            .getValue("missing_refs").jsonArray
            .map { ref -> ref.jsonObject.getValue("reason").jsonPrimitive.content }

    private fun validated(result: ToolResult): Int =
        assertSuccess(result).structured.jsonObject.getValue("validated").jsonPrimitive.int

    private fun rendered(result: ToolResult): Int =
        assertSuccess(result).structured.jsonObject.getValue("rendered").jsonPrimitive.int

    private fun resolvedFamilies(result: ToolResult): List<String> =
        assertSuccess(result).structured.jsonObject
            .getValue("resolved_refs").jsonArray
            .map { ref -> ref.jsonObject.getValue("family").jsonPrimitive.content }

    private fun resolvedIds(result: ToolResult): List<String> =
        assertSuccess(result).structured.jsonObject
            .getValue("resolved_refs").jsonArray
            .map { ref -> ref.jsonObject.getValue("id").jsonPrimitive.content }

    private fun uiCards(result: ToolResult) =
        requireNotNull(assertSuccess(result).uiStructured).jsonObject.getValue("cards").jsonArray

    private fun typedUiCards(result: ToolResult) =
        json.decodeFromJsonElement<ShowCardsUiStructured>(
            requireNotNull(assertSuccess(result).uiStructured)
        ).cards

    private fun orderCard(id: String): ShowCardsResolution.Resolved {
        val ref = ValidatedRef(index = 0, family = ShowCardFamily.Order, id = id)

        return ShowCardsResolution.Resolved(
            ref = ref,
            summary = json.encodeToJsonElement(
                OrderSummary(
                    id = id,
                    number = "#$id",
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
            ).jsonObject,
            card = ShowCardPayload(
                family = "order",
                id = id,
                title = "#$id",
                details = ShowCardDetails.Order(
                    status = "processing",
                    total = "12.34",
                    currency = "USD",
                    dateCreated = "2026-05-01T10:00:00Z",
                    customerName = "Jane Doe",
                ),
            )
        )
    }

    private fun productCard(id: String): ShowCardsResolution.Resolved {
        val ref = ValidatedRef(index = 0, family = ShowCardFamily.Product, id = id)

        return ShowCardsResolution.Resolved(
            ref = ref,
            summary = json.encodeToJsonElement(
                ProductSummary(
                    id = id,
                    name = "Socks",
                    sku = "woo-socks",
                    price = "9.99",
                    type = "simple",
                    stockStatus = "instock",
                    manageStock = true,
                    onSale = false,
                    stockQuantity = 12.0,
                )
            ).jsonObject,
            card = ShowCardPayload(
                family = "product",
                id = id,
                title = "Socks",
                details = ShowCardDetails.Product(
                    sku = "woo-socks",
                    price = "9.99",
                    stockStatus = "instock",
                    status = "publish",
                    imageUrl = PRODUCT_IMAGE_URL,
                ),
            )
        )
    }

    private fun variationCard(id: String): ShowCardsResolution.Resolved {
        val ref = ValidatedRef(index = 0, family = ShowCardFamily.Variation, id = id)

        return ShowCardsResolution.Resolved(
            ref = ref,
            summary = json.encodeToJsonElement(
                VariationSummary(
                    id = id,
                    productId = 100L,
                    variationId = 10L,
                    name = "Blue socks",
                    sku = "woo-socks-blue",
                    price = "12.99",
                    stockStatus = "instock",
                    status = "publish",
                    attributes = listOf(CompactVariationAttribute(name = "Size", option = "M")),
                )
            ).jsonObject,
            card = ShowCardPayload(
                family = "variation",
                id = id,
                title = "Variation 10",
                details = ShowCardDetails.Variation(
                    productId = 100L,
                    variationId = 10L,
                    sku = "woo-socks-blue",
                    price = "12.99",
                    stockStatus = "instock",
                    status = "publish",
                    attributes = listOf(CompactVariationAttribute(name = "Size", option = "M")),
                ),
            )
        )
    }

    private fun leakyProductCard(id: String): ShowCardsResolution.Resolved {
        val ref = ValidatedRef(index = 0, family = ShowCardFamily.Product, id = id)

        return ShowCardsResolution.Resolved(
            ref = ref,
            summary = buildJsonObject {
                put("id", id)
                put("name", "Socks")
                put("sku", "woo-socks")
                put("price", "9.99")
                put("stock_status", "instock")
                put("description", "Long description")
                put("html", "<p>Private</p>")
                put("images", buildJsonArray { add(JsonPrimitive("https://example.com/image.png")) })
                putJsonObject("metadata") {
                    put("private", "value")
                }
                putJsonObject("raw") {
                    put("entity", "json")
                }
            },
            card = ShowCardPayload(
                family = "product",
                id = id,
                title = "Socks",
                details = ShowCardDetails.Product(
                    sku = "woo-socks",
                    price = "9.99",
                    stockStatus = "instock",
                    status = "publish",
                ),
            )
        )
    }

    private fun analyticsStatsCard(id: String): ShowCardsResolution.Resolved {
        val ref = ValidatedRef(index = 0, family = ShowCardFamily.AnalyticsStats, id = id)
        val totals = buildJsonObject {
            put("total_sales", "170.35")
            put("net_revenue", "120.15")
        }
        val intervals = listOf(
            buildJsonObject {
                put("interval", "2026-05-01")
                put("date_start", "2026-05-01 00:00:00")
                putJsonObject("subtotals") {
                    put("total_sales", "170.35")
                    put("net_revenue", "120.15")
                }
            }
        )

        return ShowCardsResolution.Resolved(
            ref = ref,
            summary = buildJsonObject {
                put("id", id)
                put("after", "2026-05-01")
                put("before", "2026-05-07")
                put("currency", "USD")
                put("totals", totals)
                put("interval_subtotals", buildJsonArray { intervals.forEach { add(it) } })
            },
            card = ShowCardPayload(
                family = "analytics_stats",
                id = id,
                title = "Analytics",
                details = ShowCardDetails.AnalyticsStats(
                    after = "2026-05-01",
                    before = "2026-05-07",
                    currency = "USD",
                    totals = totals,
                    intervalSubtotals = intervals,
                ),
            )
        )
    }

    private fun customerCard(id: String): ShowCardsResolution.Resolved {
        val ref = ValidatedRef(index = 0, family = ShowCardFamily.Customer, id = id)

        return ShowCardsResolution.Resolved(
            ref = ref,
            summary = json.encodeToJsonElement(
                CustomerSummary(
                    id = id,
                    name = "Ada Lovelace",
                    email = "ada@example.com",
                )
            ).jsonObject,
            card = ShowCardPayload(
                family = "customer",
                id = id,
                title = "Ada Lovelace",
                details = ShowCardDetails.Customer(email = "ada@example.com"),
            )
        )
    }

    private fun leakyAnalyticsStatsCard(id: String): ShowCardsResolution.Resolved {
        val resolved = analyticsStatsCard(id)
        return resolved.copy(
            summary = buildJsonObject {
                resolved.summary.forEach { (key, value) -> put(key, value) }
                put("private_total", "should not leak")
                putJsonObject("debug") {
                    put("request", "raw")
                }
            }
        )
    }

    private class FakeResolver(
        private val resolutions: List<ShowCardsResolution>,
    ) : ShowCardsResolver {
        override suspend fun resolve(refs: List<ValidatedRef>): List<ShowCardsResolution> =
            refs.map { ref -> resolutions.first { it.ref.family == ref.family && it.ref.id == ref.id } }

        companion object {
            fun empty(): FakeResolver = FakeResolver(emptyList())

            fun resolving(vararg resolutions: ShowCardsResolution): FakeResolver =
                FakeResolver(resolutions.toList())

            fun returning(resolutions: List<ShowCardsResolution>): FakeResolver =
                FakeResolver(resolutions)
        }
    }

    private object ThrowingResolver : ShowCardsResolver {
        override suspend fun resolve(refs: List<ValidatedRef>): List<ShowCardsResolution> =
            error("Resolver failed")
    }

    private companion object {
        private const val PRODUCT_IMAGE_URL = "https://example.com/socks.png"
        private const val ANALYTICS_STATS_ID =
            "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day"
    }
}
