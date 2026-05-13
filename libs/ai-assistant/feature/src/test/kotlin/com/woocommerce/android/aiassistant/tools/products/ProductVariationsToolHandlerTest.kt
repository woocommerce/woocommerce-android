package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.tools.testToolFailureDiagnosticsFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductVariationModel

@OptIn(ExperimentalCoroutinesApi::class)
class ProductVariationsToolHandlerTest {

    private val dataSource: AIProductVariationsDataSource = mock()
    private val handler = ProductVariationsToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
        diagnosticsFactory = testToolFailureDiagnosticsFactory(),
    )

    private fun variation(
        productId: Long = 100L,
        variationId: Long = 10L,
        price: String = "19.99",
        stockStatus: String = "instock",
        attributes: String = "",
        description: String = "",
        weight: String = "",
        length: String = "",
        width: String = "",
        height: String = "",
        taxClass: String = "",
        dateCreated: String = "",
        dateModified: String = "",
        menuOrder: Int = 0,
        backorders: String = "",
        image: String = "",
    ) = WCProductVariationModel(
        localSiteId = LocalId(1),
        remoteProductId = RemoteId(productId),
        remoteVariationId = RemoteId(variationId),
        status = "publish",
        sku = "SKU-$variationId",
        regularPrice = price,
        salePrice = "",
        onSale = false,
        manageStock = true,
        stockQuantity = 3.0,
        stockStatus = stockStatus,
        price = price,
        attributes = attributes,
        description = description,
        weight = weight,
        length = length,
        width = width,
        height = height,
        taxClass = taxClass,
        dateCreated = dateCreated,
        dateModified = dateModified,
        menuOrder = menuOrder,
        backorders = backorders,
        image = image,
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "product_variations_list", arguments = arguments)

    @Test
    fun `given descriptor, when inspected, then broad inventory questions are excluded`() {
        val description = handler.descriptor.description

        assertThat(description).contains("explicitly asks")
        assertThat(description).contains("variations, sizes, colors, options")
        assertThat(description).contains("broad product-level inventory questions")
    }

    @Test
    fun `given list mode, when execute is called, then data source is called with product id and paging`() = runTest {
        whenever(dataSource.fetchVariations(productId = 100L, page = 2, perPage = 25))
            .thenReturn(Result.success(emptyList()))

        handler.execute(
            toolCall(
                buildJsonObject {
                    put("product_id", 100)
                    put("page", 2)
                    put("per_page", 25)
                }
            )
        )

        verify(dataSource).fetchVariations(productId = 100L, page = 2, perPage = 25)
    }

    @Test
    fun `given list mode result, when execute is called, then structured JSON contains aggregate fields`() = runTest {
        whenever(dataSource.fetchVariations(productId = 100L, page = 1, perPage = 20)).thenReturn(
            Result.success(
                listOf(
                    variation(productId = 100L, variationId = 10L, price = "19.99", stockStatus = "instock"),
                    variation(productId = 100L, variationId = 11L, price = "7.50", stockStatus = "outofstock"),
                    variation(productId = 100L, variationId = 12L, price = "12.00", stockStatus = "instock"),
                )
            )
        )

        val result = handler.execute(toolCall(buildJsonObject { put("product_id", 100) }))

        assertThat(result).isInstanceOf(ToolResult.Success::class.java)
        val json = (result as ToolResult.Success).structured.jsonObject
        assertThat(requireNotNull(json["product_id"]).jsonPrimitive.content).isEqualTo("100")
        assertThat(requireNotNull(json["count"]).jsonPrimitive.int).isEqualTo(3)
        assertThat(requireNotNull(json["ids"]).jsonArray.map { it.jsonPrimitive.content.toLong() })
            .containsExactly(10L, 11L, 12L)
        val stockCounts = requireNotNull(json["stock_status_counts"]).jsonObject
        assertThat(requireNotNull(stockCounts["instock"]).jsonPrimitive.int).isEqualTo(2)
        assertThat(requireNotNull(stockCounts["outofstock"]).jsonPrimitive.int).isEqualTo(1)
        val priceRange = requireNotNull(json["price_range"]).jsonObject
        assertThat(requireNotNull(priceRange["min"]).jsonPrimitive.content).isEqualTo("7.50")
        assertThat(requireNotNull(priceRange["max"]).jsonPrimitive.content).isEqualTo("19.99")
    }

    @Test
    fun `given list mode with no prices, when execute is called, then price range is null`() = runTest {
        whenever(dataSource.fetchVariations(productId = 100L, page = 1, perPage = 20)).thenReturn(
            Result.success(listOf(variation(productId = 100L, variationId = 10L, price = "")))
        )

        val result = handler.execute(toolCall(buildJsonObject { put("product_id", 100) }))

        val json = (result as ToolResult.Success).structured.jsonObject
        assertThat(json["price_range"]).isEqualTo(JsonNull)
    }

    @Test
    fun `given get mode, when execute is called, then variation detail fields are returned`() = runTest {
        whenever(dataSource.getVariation(productId = 100L, variationId = 10L))
            .thenReturn(
                Result.success(
                    variation(
                        productId = 100L,
                        variationId = 10L,
                        price = "19.99",
                        attributes = """[{"name":"Size","option":"M"}]""",
                    )
                )
            )

        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("product_id", 100)
                    put("variation_id", 10)
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.Success::class.java)
        val json = (result as ToolResult.Success).structured.jsonObject
        assertThat(requireNotNull(json["id"]).jsonPrimitive.content).isEqualTo("10")
        assertThat(requireNotNull(json["product_id"]).jsonPrimitive.content).isEqualTo("100")
        assertThat(requireNotNull(json["status"]).jsonPrimitive.content).isEqualTo("publish")
        assertThat(requireNotNull(json["sku"]).jsonPrimitive.content).isEqualTo("SKU-10")
        assertThat(requireNotNull(json["regular_price"]).jsonPrimitive.content).isEqualTo("19.99")
        assertThat(requireNotNull(json["stock_status"]).jsonPrimitive.content).isEqualTo("instock")
        val option = json.getValue("attributes").jsonArray.single()
            .jsonObject.getValue("option").jsonPrimitive.content
        assertThat(option).isEqualTo("M")
        verify(dataSource).getVariation(productId = 100L, variationId = 10L)
    }

    @Test
    fun `given list mode result, when execute is called, then structured JSON contains variation rows with attributes`() =
        runTest {
            whenever(dataSource.fetchVariations(productId = 100L, page = 1, perPage = 20)).thenReturn(
                Result.success(
                    listOf(
                        variation(
                            productId = 100L,
                            variationId = 10L,
                            price = "19.99",
                            stockStatus = "instock",
                            attributes = """[{"name":"Size","option":"M"}]""",
                        )
                    )
                )
            )

            val result = handler.execute(toolCall(buildJsonObject { put("product_id", 100) }))

            val row = (result as ToolResult.Success).structured.jsonObject
                .getValue("variations").jsonArray.single().jsonObject
            assertThat(row.getValue("id").jsonPrimitive.long).isEqualTo(10L)
            assertThat(row.getValue("product_id").jsonPrimitive.long).isEqualTo(100L)
            assertThat(
                row.getValue("attributes").jsonArray.single().jsonObject.getValue("option").jsonPrimitive.content
            )
                .isEqualTo("M")
        }

    @Test
    fun `given variation extras, when execute is called, then every allowed variation extra is included`() =
        runTest {
            whenever(dataSource.fetchVariations(productId = 100L, page = 1, perPage = 20)).thenReturn(
                Result.success(
                    listOf(
                        variation(
                            productId = 100L,
                            variationId = 10L,
                            price = "19.99",
                            description = "Red medium hoodie",
                            weight = "1.25",
                            length = "10",
                            width = "5",
                            height = "2",
                            taxClass = "standard",
                            dateCreated = "2026-05-01T10:00:00Z",
                            dateModified = "2026-05-02T10:00:00Z",
                            menuOrder = 3,
                            backorders = "notify",
                            image = """
                                {"id":7,"src":"https://example.com/variation.jpg","alt":"Variation","name":"Front"}
                            """.trimIndent(),
                            attributes = """[{"name":"Size","option":"M"}]""",
                        )
                    )
                )
            )

            val result = handler.execute(toolCall(buildJsonObject { put("product_id", 100) }))

            val row = (result as ToolResult.Success).structured.jsonObject
                .getValue("variations").jsonArray.single().jsonObject
            assertThat(row.getValue("image").jsonObject.getValue("src").jsonPrimitive.content)
                .isEqualTo("https://example.com/variation.jpg")
            assertThat(row.getValue("description").jsonPrimitive.content).isEqualTo("Red medium hoodie")
            assertThat(row.getValue("weight").jsonPrimitive.content).isEqualTo("1.25")
            assertThat(row.getValue("dimensions").jsonObject.getValue("height").jsonPrimitive.content).isEqualTo("2")
            assertThat(row.getValue("tax_class").jsonPrimitive.content).isEqualTo("standard")
            assertThat(row.getValue("date_created").jsonPrimitive.content).isEqualTo("2026-05-01T10:00:00Z")
            assertThat(row.getValue("date_modified").jsonPrimitive.content).isEqualTo("2026-05-02T10:00:00Z")
            assertThat(row.getValue("menu_order").jsonPrimitive.int).isEqualTo(3)
            assertThat(row.getValue("backorders").jsonPrimitive.content).isEqualTo("notify")
        }

    @Test
    fun `given long variation description extra, when list executes, then description is capped with marker`() =
        runTest {
            val longDescription = "x".repeat(PRODUCT_TEXT_FIELD_LIMIT + 1)
            whenever(dataSource.fetchVariations(productId = 100L, page = 1, perPage = 20)).thenReturn(
                Result.success(
                    listOf(
                        variation(
                            productId = 100L,
                            variationId = 10L,
                            description = longDescription,
                        )
                    )
                )
            )

            val result = handler.execute(toolCall(buildJsonObject { put("product_id", 100) }))

            val row = (result as ToolResult.Success).structured.jsonObject
                .getValue("variations").jsonArray.single().jsonObject
            assertThat(row.getValue("description").jsonPrimitive.content).hasSize(PRODUCT_TEXT_FIELD_LIMIT)
            assertThat(row.getValue("description_truncated").jsonPrimitive.boolean).isTrue
        }

    @Test
    fun `given get mode, when execute is called, then page arguments are ignored and get variation is used`() =
        runTest {
            whenever(dataSource.getVariation(productId = 100L, variationId = 10L))
                .thenReturn(Result.success(variation(productId = 100L, variationId = 10L)))

            handler.execute(
                toolCall(
                    buildJsonObject {
                        put("product_id", 100)
                        put("variation_id", 10)
                        put("page", 9)
                        put("per_page", 99)
                    }
                )
            )

            verify(dataSource).getVariation(productId = 100L, variationId = 10L)
        }

    @Test
    fun `given data source failure, when execute is called, then retryable transport error is returned`() = runTest {
        whenever(dataSource.fetchVariations(productId = 100L, page = 1, perPage = 20))
            .thenReturn(Result.failure(RuntimeException("boom")))

        val result = handler.execute(toolCall(buildJsonObject { put("product_id", 100) }))

        assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
        assertThat((result as ToolResult.TransportError).retryable).isTrue
    }

    @Test
    fun `given missing product id, when execute is called, then validation error is returned`() = runTest {
        val result = handler.execute(toolCall(buildJsonObject { }))

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }
}
