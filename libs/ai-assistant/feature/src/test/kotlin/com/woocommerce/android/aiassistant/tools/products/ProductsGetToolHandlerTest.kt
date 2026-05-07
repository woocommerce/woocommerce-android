package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel

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
    fun `given all product detail extras, when execute is called, then structured JSON includes every requested extra`() =
        runTest {
            val product = WCProductModel(
                remoteId = RemoteId(42L),
                name = "Hoodie",
                type = "simple",
                description = "Long description",
                shortDescription = "Short description",
                attributes = """[{"id":1,"name":"Size","visible":true,"variation":true,"options":["M","L"]}]""",
                images = """[{"id":7,"src":"https://example.com/hoodie.jpg","alt":"Hoodie","name":"Front"}]""",
                length = "10",
                width = "5",
                height = "2",
                weight = "1.25",
                shippingClass = "shirts",
                crossSellIds = "[11,12]",
                upsellIds = "[21]",
                relatedIds = "[31,32]",
            )
            whenever(dataSource.getProduct(productId = 42L)).thenReturn(Result.success(product))

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("id", 42)
                        putJsonArray("extra_fields") {
                            add("description")
                            add("short_description")
                            add("attributes")
                            add("images")
                            add("dimensions")
                            add("weight")
                            add("shipping_class")
                            add("cross_sell_ids")
                            add("upsell_ids")
                            add("related_ids")
                        }
                    }
                )
            )

            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(json.getValue("description").jsonPrimitive.content).isEqualTo("Long description")
            assertThat(json.getValue("short_description").jsonPrimitive.content).isEqualTo("Short description")
            assertThat(json.getValue("attributes").jsonArray.single().jsonObject.getValue("name").jsonPrimitive.content)
                .isEqualTo("Size")
            assertThat(json.getValue("images").jsonArray.single().jsonObject.getValue("src").jsonPrimitive.content)
                .isEqualTo("https://example.com/hoodie.jpg")
            assertThat(json.getValue("dimensions").jsonObject.getValue("length").jsonPrimitive.content).isEqualTo("10")
            assertThat(json.getValue("weight").jsonPrimitive.content).isEqualTo("1.25")
            assertThat(json.getValue("shipping_class").jsonPrimitive.content).isEqualTo("shirts")
            assertThat(json.getValue("cross_sell_ids").jsonArray.map { it.jsonPrimitive.long })
                .containsExactly(11L, 12L)
            assertThat(json.getValue("upsell_ids").jsonArray.map { it.jsonPrimitive.long }).containsExactly(21L)
            assertThat(json.getValue("related_ids").jsonArray.map { it.jsonPrimitive.long }).containsExactly(31L, 32L)
        }

    @Test
    fun `given unsupported extra field, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("id", 42)
                    putJsonArray("extra_fields") { add("raw_html") }
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        assertThat((result as ToolResult.ValidationError).reason)
            .contains("Unsupported products_get extra_fields: raw_html")
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
    fun `given data source fails, when execute is called, then retryable TransportError is returned`() = runTest {
        whenever(dataSource.getProduct(99L)).thenReturn(Result.failure(RuntimeException("not found")))

        val result = handler.execute(toolCall(buildJsonObject { put("id", 99) }))

        assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
        assertThat((result as ToolResult.TransportError).retryable).isTrue
    }
}
