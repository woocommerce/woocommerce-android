package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsListToolHandlerTest {

    private val dataSource: AIProductsDataSource = mock()
    private val handler = ProductsListToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
    )

    private fun makeProduct(
        id: Long = 1L,
        status: String = "publish",
        name: String = "",
        sku: String = "",
        price: String = "",
        stockStatus: String = "",
        type: String = "",
    ): WCProductModel = WCProductModel(
        remoteId = RemoteId(id),
        status = status,
        name = name,
        sku = sku,
        price = price,
        stockStatus = stockStatus,
        type = type,
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "products_list", arguments = arguments)

    @Test
    fun `given products are returned, when execute is called, then structured JSON contains count and ids`() =
        runTest {
            val product1 = makeProduct(id = 10L)
            val product2 = makeProduct(id = 20L)
            whenever(dataSource.fetchProducts(search = null)).thenReturn(
                Result.success(
                    AIProductsDataSource.ProductsPage(products = listOf(product1, product2), canLoadMore = false)
                )
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(requireNotNull(json["count"]).jsonPrimitive.int).isEqualTo(2)
            val ids = requireNotNull(json["ids"]).jsonArray.map { it.jsonPrimitive.content.toLong() }
            assertThat(ids).containsExactly(10L, 20L)
        }

    @Test
    fun `given products are returned, when execute is called, then structured JSON contains per row summaries`() =
        runTest {
            val product = makeProduct(
                id = 10L,
                name = "Socks",
                sku = "SOCK",
                price = "9.99",
                stockStatus = "instock",
                type = "simple",
                status = "publish",
            )
            whenever(dataSource.fetchProducts(search = null)).thenReturn(
                Result.success(AIProductsDataSource.ProductsPage(products = listOf(product), canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            val row = (result as ToolResult.Success).structured.jsonObject
                .getValue("products").jsonArray.single().jsonObject
            assertThat(row.getValue("id").jsonPrimitive.long).isEqualTo(10L)
            assertThat(row.getValue("name").jsonPrimitive.content).isEqualTo("Socks")
            assertThat(row.getValue("sku").jsonPrimitive.content).isEqualTo("SOCK")
            assertThat(row.getValue("price").jsonPrimitive.content).isEqualTo("9.99")
            assertThat(row.getValue("stock_status").jsonPrimitive.content).isEqualTo("instock")
            assertThat(row.getValue("type").jsonPrimitive.content).isEqualTo("simple")
            assertThat(row.getValue("status").jsonPrimitive.content).isEqualTo("publish")
        }

    @Test
    fun `given product list extras, when execute is called, then every allowed list extra is included`() =
        runTest {
            val product = WCProductModel(
                remoteId = RemoteId(10L),
                name = "Socks",
                sku = "SOCK",
                price = "9.99",
                regularPrice = "12.99",
                salePrice = "9.99",
                onSale = true,
                stockQuantity = 4.0,
                manageStock = true,
                stockStatus = "instock",
                type = "simple",
                status = "publish",
                categories = """[{"id":1,"name":"Clothing","slug":"clothing"}]""",
                tags = """[{"id":2,"name":"Featured","slug":"featured"}]""",
                totalSales = 20L,
                dateCreated = "2026-05-01T10:00:00Z",
                dateModified = "2026-05-02T10:00:00Z",
                images = """[{"id":7,"src":"https://example.com/socks.jpg","alt":"Socks","name":"Front"}]""",
                shortDescription = "Short socks",
                description = "Long socks",
            )
            whenever(dataSource.fetchProducts(search = null)).thenReturn(
                Result.success(AIProductsDataSource.ProductsPage(products = listOf(product), canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            val row = (result as ToolResult.Success).structured.jsonObject
                .getValue("products").jsonArray.single().jsonObject
            assertProductListExtras(row)
        }

    @Test
    fun `given products with mixed statuses, when execute is called, then status_counts is correct`() =
        runTest {
            val products = listOf(
                makeProduct(id = 1L, status = "publish"),
                makeProduct(id = 2L, status = "publish"),
                makeProduct(id = 3L, status = "draft"),
            )
            whenever(dataSource.fetchProducts(search = null)).thenReturn(
                Result.success(AIProductsDataSource.ProductsPage(products = products, canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            val json = (result as ToolResult.Success).structured.jsonObject
            val statusCounts = requireNotNull(json["status_counts"]).jsonObject
            assertThat(requireNotNull(statusCounts["publish"]).jsonPrimitive.int).isEqualTo(2)
            assertThat(requireNotNull(statusCounts["draft"]).jsonPrimitive.int).isEqualTo(1)
        }

    @Test
    fun `given empty result, when execute is called, then count is zero`() =
        runTest {
            whenever(dataSource.fetchProducts(search = null)).thenReturn(
                Result.success(AIProductsDataSource.ProductsPage(products = emptyList(), canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(requireNotNull(json["count"]).jsonPrimitive.int).isEqualTo(0)
        }

    @Test
    fun `given data source fails, when execute is called, then retryable TransportError is returned`() =
        runTest {
            whenever(dataSource.fetchProducts(search = null)).thenReturn(
                Result.failure(RuntimeException("network error"))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
            assertThat((result as ToolResult.TransportError).retryable).isTrue
        }

    @Test
    fun `given search argument is an integer, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(toolCall(buildJsonObject { put("search", 42) }))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        }

    private fun assertProductListExtras(row: JsonObject) {
        val category = row.getValue("categories").jsonArray.single().jsonObject
        val tag = row.getValue("tags").jsonArray.single().jsonObject
        val image = row.getValue("image").jsonObject

        assertThat(row.getValue("regular_price").jsonPrimitive.content).isEqualTo("12.99")
        assertThat(row.getValue("sale_price").jsonPrimitive.content).isEqualTo("9.99")
        assertThat(row.getValue("on_sale").jsonPrimitive.boolean).isTrue
        assertThat(row.getValue("stock_quantity").jsonPrimitive.double).isEqualTo(4.0)
        assertThat(row.getValue("manage_stock").jsonPrimitive.boolean).isTrue
        assertThat(category.getValue("name").jsonPrimitive.content).isEqualTo("Clothing")
        assertThat(tag.getValue("name").jsonPrimitive.content).isEqualTo("Featured")
        assertThat(row.getValue("total_sales").jsonPrimitive.long).isEqualTo(20L)
        assertThat(row.getValue("date_created").jsonPrimitive.content).isEqualTo("2026-05-01T10:00:00Z")
        assertThat(row.getValue("date_modified").jsonPrimitive.content).isEqualTo("2026-05-02T10:00:00Z")
        assertThat(image.getValue("src").jsonPrimitive.content).isEqualTo("https://example.com/socks.jpg")
        assertThat(row.getValue("short_description").jsonPrimitive.content).isEqualTo("Short socks")
        assertThat(row.getValue("description").jsonPrimitive.content).isEqualTo("Long socks")
    }
}
