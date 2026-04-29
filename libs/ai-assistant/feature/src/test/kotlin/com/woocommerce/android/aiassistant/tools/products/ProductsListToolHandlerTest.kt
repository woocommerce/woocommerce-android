package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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

    private fun makeProduct(id: Long = 1L, status: String = "publish"): WCProductModel = mock {
        on { remoteProductId }.thenReturn(id)
        on { this.status }.thenReturn(status)
    }

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "products_list", arguments = arguments)

    @Test
    fun `given no search argument, when execute is called, then data source is called with search = null`() =
        runTest {
            whenever(dataSource.fetchProducts(search = null)).thenReturn(
                Result.success(AIProductsDataSource.ProductsPage(products = emptyList(), canLoadMore = false))
            )

            handler.execute(toolCall(buildJsonObject {}))

            verify(dataSource).fetchProducts(search = null)
        }

    @Test
    fun `given search = shirt, when execute is called, then data source is called with search = shirt`() =
        runTest {
            whenever(dataSource.fetchProducts(search = "shirt")).thenReturn(
                Result.success(AIProductsDataSource.ProductsPage(products = emptyList(), canLoadMore = false))
            )

            handler.execute(toolCall(buildJsonObject { put("search", "shirt") }))

            verify(dataSource).fetchProducts(search = "shirt")
        }

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
}
