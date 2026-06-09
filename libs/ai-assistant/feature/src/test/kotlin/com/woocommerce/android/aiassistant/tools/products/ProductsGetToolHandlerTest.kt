package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.tools.testToolFailureDiagnosticsFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsGetToolHandlerTest {

    private val dataSource: AIProductsDataSource = mock()
    private val handler = ProductsGetToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
        diagnosticsFactory = testToolFailureDiagnosticsFactory(),
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "products_get", arguments = arguments)

    private fun makeProduct(): WCProductModel = WCProductModel(
        remoteId = RemoteId(42L),
        name = "Blue T-Shirt",
        status = "publish",
        type = "simple",
        sku = "SHIRT-BLUE",
        regularPrice = "29.99",
        salePrice = "",
        onSale = false,
        manageStock = true,
        stockQuantity = 15.0,
        stockStatus = "instock",
        dateCreated = "2024-03-01T00:00:00",
    )

    @Test
    fun `given descriptor, when inspected, then iOS-aligned fetch guidance is exposed`() {
        val description = handler.descriptor.description
        val properties = handler.descriptor.inputSchema.getValue("properties").jsonObject

        assertThat(description).contains("full description")
        assertThat(description).contains("categories")
        assertThat(description).contains("fields not shown on product cards")
        assertThat(description).contains("prior-turn product position/reference")
        assertThat(description).contains("use product_variations_list instead")
        assertThat(description).contains("variations, sizes, colors")
        assertThat(description).contains("Do not call just to re-render an existing product card")
        assertThat(description).doesNotContain("Do NOT call this tool to render a card")
        assertThat(properties.keys).containsExactly("id")
        assertThat(handler.descriptor.inputSchema.getValue("required").jsonArray.map { it.jsonPrimitive.content })
            .containsExactly("id")
    }

    @Test
    fun `given a valid id, when execute is called, then structured JSON contains expected fields`() = runTest {
        val product = makeProduct()
        whenever(dataSource.getProduct(42L)).thenReturn(Result.success(product))

        val result = handler.execute(toolCall(buildJsonObject { put("id", 42) }))

        assertThat(result).isInstanceOf(ToolResult.Success::class.java)
        val json = (result as ToolResult.Success).structured.jsonObject

        assertThat(requireNotNull(json["id"]).jsonPrimitive.content).isEqualTo("42")
        assertThat(requireNotNull(json["name"]).jsonPrimitive.content).isEqualTo("Blue T-Shirt")
        assertThat(requireNotNull(json["status"]).jsonPrimitive.content).isEqualTo("publish")
        assertThat(requireNotNull(json["sku"]).jsonPrimitive.content).isEqualTo("SHIRT-BLUE")
        assertThat(requireNotNull(json["regular_price"]).jsonPrimitive.content).isEqualTo("29.99")
        assertThat(requireNotNull(json["stock_status"]).jsonPrimitive.content).isEqualTo("instock")
    }

    @Test
    fun `given widened product detail field, when execute is called, then structured JSON includes it`() =
        runTest {
            val product = WCProductModel(
                remoteId = RemoteId(42L),
                name = "Hoodie",
                type = "simple",
                description = "Long description",
            )
            whenever(dataSource.getProduct(productId = 42L)).thenReturn(Result.success(product))

            val result = handler.execute(toolCall(buildJsonObject { put("id", 42) }))

            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(json.getValue("description").jsonPrimitive.content).isEqualTo("Long description")
        }

    @Test
    fun `given unknown argument, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("id", 42)
                    put("unexpected", true)
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }

    @Test
    fun `given missing id, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(toolCall(buildJsonObject {}))

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }

    @Test
    fun `given data source fails, when execute is called, then lossy diagnostics are returned`() = runTest {
        val error = OnChangedException(WCProductStore.ProductError(message = "not found"))
        whenever(dataSource.getProduct(99L)).thenReturn(Result.failure(error))

        val result = handler.execute(toolCall(buildJsonObject { put("id", 99) }))

        assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
        result as ToolResult.TransportError
        assertThat(result.retryable).isTrue
        assertThat(result.diagnostics.tool?.toolName).isEqualTo("products_get")
        assertThat(result.diagnostics.transport).isNull()
    }
}
