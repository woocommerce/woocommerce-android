package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.tools.handlers.cards.OrderSummary
import com.woocommerce.android.aiassistant.tools.handlers.cards.ProductSummary
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardFamily
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsRejectionReason
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolution
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolver
import com.woocommerce.android.aiassistant.tools.handlers.cards.ValidatedRef
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
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
    fun `when descriptor is inspected, then show cards accepts Android v1 order and product references`() {
        val descriptor = handler.descriptor

        assertThat(descriptor.name).isEqualTo("show_cards")
        assertThat(descriptor.description).contains("order")
        assertThat(descriptor.description).contains("product")
        assertThat(descriptor.inputSchema.toString()).contains("references")
        assertThat(descriptor.inputSchema.toString()).contains("family")
        assertThat(descriptor.inputSchema.toString()).contains("id")
        assertThat(descriptor.inputSchema.toString()).contains("order")
        assertThat(descriptor.inputSchema.toString()).contains("product")
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

        assertThat(summary.keys).containsExactly("id", "number", "status", "total", "currency", "date_created")
    }

    @Test
    fun `given resolved product, when executed, then summary contains only allowlisted fields`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(productCard(id = "456")),
            referencesJson = """[{ "family": "product", "id": "456" }]"""
        )

        val summary = firstResolvedSummary(result)

        assertThat(summary.keys).containsExactly("id", "name", "sku", "price", "stock_status")
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

        val card = uiCards(result).single().jsonObject
        val details = card.getValue("details").jsonObject

        assertThat(card.keys).containsExactly("family", "id", "title", "details")
        assertThat(card.getValue("family").jsonPrimitive.content).isEqualTo("order")
        assertThat(card.getValue("id").jsonPrimitive.content).isEqualTo("123")
        assertThat(card.getValue("title").jsonPrimitive.content).isEqualTo("#123")
        assertThat(details.keys).containsExactly("kind", "status", "total", "currency", "date_created")
        assertThat(details.getValue("kind").jsonPrimitive.content).isEqualTo("order")
        assertThat(details.getValue("status").jsonPrimitive.content).isEqualTo("processing")
        assertThat(details.getValue("total").jsonPrimitive.content).isEqualTo("12.34")
        assertThat(details.getValue("currency").jsonPrimitive.content).isEqualTo("USD")
        assertThat(details.getValue("date_created").jsonPrimitive.content).isEqualTo("2026-05-01T10:00:00Z")
        assertThat(card.keys).doesNotContain("subtitle", "badges", "attributes")
        assertThat(assertSuccess(result).structured.toString()).doesNotContain("details")
    }

    @Test
    fun `given resolved product, when executed, then uiStructured contains typed product details`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(productCard(id = "456")),
            referencesJson = """[{ "family": "product", "id": "456" }]"""
        )

        val card = uiCards(result).single().jsonObject
        val details = card.getValue("details").jsonObject

        assertThat(card.keys).containsExactly("family", "id", "title", "details")
        assertThat(card.getValue("family").jsonPrimitive.content).isEqualTo("product")
        assertThat(card.getValue("id").jsonPrimitive.content).isEqualTo("456")
        assertThat(card.getValue("title").jsonPrimitive.content).isEqualTo("Socks")
        assertThat(details.keys).containsExactly("kind", "sku", "price", "stock_status", "status")
        assertThat(details.getValue("kind").jsonPrimitive.content).isEqualTo("product")
        assertThat(details.getValue("sku").jsonPrimitive.content).isEqualTo("woo-socks")
        assertThat(details.getValue("price").jsonPrimitive.content).isEqualTo("9.99")
        assertThat(details.getValue("stock_status").jsonPrimitive.content).isEqualTo("instock")
        assertThat(details.getValue("status").jsonPrimitive.content).isEqualTo("publish")
        assertThat(card.keys).doesNotContain("subtitle", "badges", "attributes")
        assertThat(assertSuccess(result).structured.toString()).doesNotContain("details")
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
                { "family": "customer", "id": "1" },
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
                { "family": "order", "id": null }
              ]
            }
            """.trimIndent()
        )

        assertThat(rejectedReasons(result)).containsExactly("invalid_id", "invalid_id", "invalid_id")
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
                { "family": "order", "id": "0" }
              ]
            }
            """.trimIndent()
        )

        assertThat(validated(result)).isEqualTo(0)
        assertThat(rejectedReasons(result)).containsExactly("invalid_id", "invalid_id", "invalid_id")
    }

    @Test
    fun `given duplicate refs, when executed, then duplicates after first family id pair are rejected`() = runTest {
        val result = callShowCards(
            resolver = FakeResolver.resolving(orderCard(id = "123"), productCard(id = "123")),
            referencesJson = """
                [
                  { "family": "order", "id": "123" },
                  { "family": "order", "id": "123" },
                  { "family": "product", "id": "123" }
                ]
            """.trimIndent()
        )

        assertThat(validated(result)).isEqualTo(2)
        assertThat(rejectedReasons(result)).containsExactly("duplicate_ref")
        assertThat(resolvedFamilies(result)).containsExactly("order", "product")
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
                    stockStatus = "instock",
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
                put("images", buildJsonArray { add("https://example.com/image.png") })
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
}
