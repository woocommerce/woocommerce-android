package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductVariationModel

@OptIn(ExperimentalCoroutinesApi::class)
class ProductVariationsUpdateToolHandlerTest {

    private val dataSource: AIProductVariationsDataSource = mock()
    private val handler = ProductVariationsUpdateToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
    )

    private fun variation(
        productId: Long = 100L,
        variationId: Long = 10L,
        attributes: String = "",
    ) = WCProductVariationModel(
        localSiteId = LocalId(1),
        remoteProductId = RemoteId(productId),
        remoteVariationId = RemoteId(variationId),
        status = "private",
        sku = "SKU-10",
        regularPrice = "29.99",
        salePrice = "24.99",
        onSale = true,
        manageStock = true,
        stockQuantity = 7.0,
        stockStatus = "onbackorder",
        price = "24.99",
        attributes = attributes,
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "product_variations_update", arguments = arguments)

    @Test
    fun `given descriptor, when inspected, then schema contains only supported variation update fields`() {
        val schema = handler.descriptor.inputSchema
        val properties = requireNotNull(schema["properties"]).jsonObject
        val required = requireNotNull(schema["required"]).jsonArray.map { it.jsonPrimitive.content }
        val stockStatusEnum = requireNotNull(properties["stock_status"]).jsonObject.getValue("enum").jsonArray
            .map { it.jsonPrimitive.content }
        val statusEnum = requireNotNull(properties["status"]).jsonObject.getValue("enum").jsonArray
            .map { it.jsonPrimitive.content }

        assertThat(handler.descriptor.name).isEqualTo("product_variations_update")
        assertThat(handler.descriptor.safetyLevel).isEqualTo(ToolSafetyLevel.UNSAFE)
        assertThat(handler.descriptor.description).contains("product_id")
        assertThat(handler.descriptor.description).contains("stock_quantity")
        assertThat(handler.descriptor.description).contains("After a successful update")
        assertThat(handler.descriptor.description).contains("show_cards")
        assertThat(handler.descriptor.description).contains("strict {parentProductId}/{variationId}")
        assertThat(requireNotNull(schema["additionalProperties"]).jsonPrimitive.boolean).isFalse
        assertThat(properties.keys).containsExactlyInAnyOrder(
            "product_id",
            "id",
            "regular_price",
            "sale_price",
            "stock_quantity",
            "stock_status",
            "sku",
            "status",
        )
        assertThat(required).containsExactly("product_id", "id")
        assertThat(stockStatusEnum).containsExactly("instock", "outofstock", "onbackorder")
        assertThat(statusEnum).containsExactly("draft", "pending", "private", "publish")
    }

    @Test
    fun `given valid fields, when execute is called, then updateVariation is called and detail JSON is returned`() =
        runTest {
            whenever(
                dataSource.updateVariation(
                    productId = 100L,
                    variationId = 10L,
                    update = AIProductVariationsDataSource.VariationUpdate(
                        regularPrice = "29.99",
                        salePrice = "24.99",
                        stockQuantity = 7,
                        stockStatus = "onbackorder",
                        sku = "SKU-10",
                        status = "private",
                    ),
                )
            ).thenReturn(Result.success(variation()))

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("product_id", 100)
                        put("id", 10)
                        put("regular_price", "29.99")
                        put("sale_price", "24.99")
                        put("stock_quantity", 7)
                        put("stock_status", "onbackorder")
                        put("sku", "SKU-10")
                        put("status", "private")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(requireNotNull(json["id"]).jsonPrimitive.long).isEqualTo(10L)
            assertThat(requireNotNull(json["product_id"]).jsonPrimitive.long).isEqualTo(100L)
            assertThat(requireNotNull(json["status"]).jsonPrimitive.content).isEqualTo("private")
            assertThat(requireNotNull(json["sku"]).jsonPrimitive.content).isEqualTo("SKU-10")
            assertThat(requireNotNull(json["regular_price"]).jsonPrimitive.content).isEqualTo("29.99")
            assertThat(requireNotNull(json["sale_price"]).jsonPrimitive.content).isEqualTo("24.99")
            assertThat(requireNotNull(json["on_sale"]).jsonPrimitive.boolean).isTrue
            assertThat(requireNotNull(json["manage_stock"]).jsonPrimitive.boolean).isTrue
            assertThat(requireNotNull(json["stock_quantity"]).jsonPrimitive.content).isEqualTo("7.0")
            assertThat(requireNotNull(json["stock_status"]).jsonPrimitive.content).isEqualTo("onbackorder")
            verify(dataSource).updateVariation(
                productId = 100L,
                variationId = 10L,
                update = AIProductVariationsDataSource.VariationUpdate(
                    regularPrice = "29.99",
                    salePrice = "24.99",
                    stockQuantity = 7,
                    stockStatus = "onbackorder",
                    sku = "SKU-10",
                    status = "private",
                ),
            )
        }

    @Test
    fun `given variation update succeeds, when execute is called, then widened variation detail is returned`() =
        runTest {
            val variation = variation(
                productId = 100L,
                variationId = 10L,
                attributes = """[{"name":"Size","option":"M"}]""",
            )
            whenever(
                dataSource.updateVariation(
                    productId = 100L,
                    variationId = 10L,
                    update = AIProductVariationsDataSource.VariationUpdate(sku = "SKU-M")
                )
            ).thenReturn(Result.success(variation))

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("product_id", 100)
                        put("id", 10)
                        put("sku", "SKU-M")
                    }
                )
            )

            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(json.getValue("id").jsonPrimitive.long).isEqualTo(10L)
            assertThat(json.getValue("attributes").jsonArray.single().jsonObject.getValue("name").jsonPrimitive.content)
                .isEqualTo("Size")
        }

    @Test
    fun `given unknown argument, when variation update executes, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("product_id", 100)
                    put("id", 10)
                    put("sku", "SKU-M")
                    put("unexpected", true)
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).updateVariation(any(), any(), any())
    }

    @Test
    fun `given missing product id, when execute is called, then ValidationError is returned before datasource access`() =
        runTest {
            val result = handler.execute(toolCall(buildJsonObject { put("id", 10) }))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            verify(dataSource, never()).updateVariation(productId = any(), variationId = any(), update = any())
        }

    @Test
    fun `given missing id, when execute is called, then ValidationError is returned before datasource access`() =
        runTest {
            val result = handler.execute(toolCall(buildJsonObject { put("product_id", 100) }))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            verify(dataSource, never()).updateVariation(productId = any(), variationId = any(), update = any())
        }

    @Test
    fun `given no editable fields, when execute is called, then ValidationError is returned before datasource access`() =
        runTest {
            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("product_id", 100)
                        put("id", 10)
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            verify(dataSource, never()).updateVariation(productId = any(), variationId = any(), update = any())
        }

    @Test
    fun `given unknown only editable field, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("product_id", 100)
                    put("id", 10)
                    put("unsupported_field", "ignored")
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).updateVariation(productId = any(), variationId = any(), update = any())
    }

    @Test
    fun `given unsupported stock status, when execute is called, then ValidationError is returned before datasource access`() =
        runTest {
            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("product_id", 100)
                        put("id", 10)
                        put("stock_status", "invalid")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            verify(dataSource, never()).updateVariation(productId = any(), variationId = any(), update = any())
        }

    @Test
    fun `given unsupported status, when execute is called, then ValidationError is returned before datasource access`() =
        runTest {
            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("product_id", 100)
                        put("id", 10)
                        put("status", "trash")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            verify(dataSource, never()).updateVariation(productId = any(), variationId = any(), update = any())
        }

    @Test
    fun `given datasource failure, when execute is called, then retryable TransportError is returned`() = runTest {
        whenever(
            dataSource.updateVariation(
                productId = 100L,
                variationId = 10L,
                update = AIProductVariationsDataSource.VariationUpdate(sku = "SKU-10"),
            )
        ).thenReturn(Result.failure(RuntimeException("network error")))

        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("product_id", 100)
                    put("id", 10)
                    put("sku", "SKU-10")
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
        assertThat((result as ToolResult.TransportError).retryable).isTrue
    }
}
