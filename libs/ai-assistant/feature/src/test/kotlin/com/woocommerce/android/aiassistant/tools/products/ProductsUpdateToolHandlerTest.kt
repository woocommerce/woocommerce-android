package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsUpdateToolHandlerTest {

    private val dataSource: AIProductsDataSource = mock()
    private val handler = ProductsUpdateToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "products_update", arguments = arguments)

    private fun product(
        id: Long = 42L,
        name: String = "Updated product",
        regularPrice: String = "12.50",
        salePrice: String = "",
        manageStock: Boolean = true,
        stockQuantity: Double = 8.0,
        status: String = "publish",
    ) = WCProductModel(
        remoteId = RemoteId(id),
        name = name,
        type = "simple",
        regularPrice = regularPrice,
        salePrice = salePrice,
        manageStock = manageStock,
        stockQuantity = stockQuantity,
        status = status,
        sku = "updated-sku",
        stockStatus = "instock",
        dateCreated = "2026-04-30T10:00:00",
    )

    @Test
    fun `given descriptor, when inspected, then schema contains only the supported product update fields`() {
        val schema = handler.descriptor.inputSchema
        val properties = requireNotNull(schema["properties"]).jsonObject
        val required = requireNotNull(schema["required"]).jsonArray.map { it.jsonPrimitive.content }

        assertThat(handler.descriptor.name).isEqualTo("products_update")
        assertThat(handler.descriptor.safetyLevel).isEqualTo(ToolSafetyLevel.UNSAFE)
        assertThat(handler.descriptor.description).contains("simple product")
        assertThat(handler.descriptor.description).contains("Accepts only these fields")
        assertThat(handler.descriptor.description).contains("name")
        assertThat(handler.descriptor.description).contains("regular_price")
        assertThat(handler.descriptor.description).contains("sale_price")
        assertThat(handler.descriptor.description).contains("stock_quantity")
        assertThat(handler.descriptor.description).contains("status")
        assertThat(handler.descriptor.description).contains("Variable products")
        assertThat(handler.descriptor.description).contains("variations")
        assertThat(handler.descriptor.description).contains("confirmation")
        assertThat(handler.descriptor.description).contains("one write")
        assertThat(properties.keys).containsExactlyInAnyOrder(
            "id",
            "name",
            "regular_price",
            "sale_price",
            "stock_quantity",
            "status",
        )
        assertThat(required).containsExactly("id")
    }

    @Test
    fun `given only id, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(toolCall(buildJsonObject { put("id", 42) }))

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).updateProduct(
            productId = any(),
            update = any(),
        )
    }

    @Test
    fun `given unsupported status, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("id", 42)
                    put("status", "trash")
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }

    @Test
    fun `given valid fields, when execute is called, then updateProduct is called and product detail is returned`() =
        runTest {
            whenever(
                dataSource.updateProduct(
                    productId = 42L,
                    update = AIProductsDataSource.ProductUpdate(
                        name = "Updated product",
                        stockQuantity = 8,
                        status = "publish",
                    )
                )
            ).thenReturn(Result.success(product()))

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("id", 42)
                        put("name", "Updated product")
                        put("stock_quantity", 8)
                        put("status", "publish")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(requireNotNull(json["id"]).jsonPrimitive.long).isEqualTo(42L)
            assertThat(requireNotNull(json["name"]).jsonPrimitive.content).isEqualTo("Updated product")
            assertThat(requireNotNull(json["status"]).jsonPrimitive.content).isEqualTo("publish")
            assertThat(requireNotNull(json["type"]).jsonPrimitive.content).isEqualTo("simple")
            assertThat(requireNotNull(json["sku"]).jsonPrimitive.content).isEqualTo("updated-sku")
            assertThat(requireNotNull(json["regular_price"]).jsonPrimitive.content).isEqualTo("12.50")
            assertThat(requireNotNull(json["stock_quantity"]).jsonPrimitive.content).isEqualTo("8.0")
            assertThat(requireNotNull(json["manage_stock"]).jsonPrimitive.content).isEqualTo("true")
            assertThat(requireNotNull(json["stock_status"]).jsonPrimitive.content).isEqualTo("instock")
            assertThat(json["product_id"]).isNull()
            verify(dataSource).updateProduct(
                productId = 42L,
                update = AIProductsDataSource.ProductUpdate(
                    name = "Updated product",
                    stockQuantity = 8,
                    status = "publish",
                )
            )
        }

    @Test
    fun `given variable product failure, when execute is called, then ValidationError is returned`() = runTest {
        whenever(
            dataSource.updateProduct(
                productId = 42L,
                update = AIProductsDataSource.ProductUpdate(name = "Updated product")
            )
        ).thenReturn(
            Result.failure(AIProductsDataSource.UnsupportedProductTypeException(42L, "variable"))
        )

        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("id", 42)
                    put("name", "Updated product")
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }

    @Test
    fun `given update fails, when execute is called, then retryable TransportError is returned`() = runTest {
        whenever(
            dataSource.updateProduct(
                productId = 42L,
                update = AIProductsDataSource.ProductUpdate(regularPrice = "12.50")
            )
        ).thenReturn(Result.failure(RuntimeException("network error")))

        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("id", 42)
                    put("regular_price", "12.50")
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
        assertThat((result as ToolResult.TransportError).retryable).isTrue
    }
}
