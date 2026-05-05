package com.woocommerce.android.aiassistant.tools.orders

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
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersListToolHandlerTest {

    private val dataSource: AIOrdersDataSource = mock()
    private val handler = OrdersListToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
    )

    private fun makeOrder(
        id: Long = 123L,
        status: String = "processing",
        total: String = "45.00",
        currency: String = "USD",
    ): OrderEntity = OrderEntity(
        localSiteId = LocalId(1),
        orderId = id,
        status = status,
        total = total,
        currency = currency,
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "list_orders", arguments = arguments)

    @Test
    fun `given orders are returned, when execute is called, then structured JSON contains count and ids`() =
        runTest {
            val order1 = makeOrder(id = 10L, status = "processing", total = "45.00")
            val order2 = makeOrder(id = 20L, status = "completed", total = "120.00")
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.success(AIOrdersDataSource.OrdersPage(orders = listOf(order1, order2), canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val json = (result as ToolResult.Success).structured.jsonObject

            assertThat(requireNotNull(json["count"]).jsonPrimitive.int).isEqualTo(2)
            val ids = requireNotNull(json["ids"]).jsonArray.map { it.jsonPrimitive.content.toLong() }
            assertThat(ids).containsExactly(10L, 20L)
        }

    @Test
    fun `given orders with mixed statuses, when execute is called, then status_counts is correct`() =
        runTest {
            val orders = listOf(
                makeOrder(id = 1L, status = "processing"),
                makeOrder(id = 2L, status = "processing"),
                makeOrder(id = 3L, status = "completed"),
            )
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.success(AIOrdersDataSource.OrdersPage(orders = orders, canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            val json = (result as ToolResult.Success).structured.jsonObject
            val statusCounts = requireNotNull(json["status_counts"]).jsonObject
            assertThat(requireNotNull(statusCounts["processing"]).jsonPrimitive.int).isEqualTo(2)
            assertThat(requireNotNull(statusCounts["completed"]).jsonPrimitive.int).isEqualTo(1)
        }

    @Test
    fun `given orders, when execute is called, then total_range reflects min and max totals`() =
        runTest {
            val orders = listOf(
                makeOrder(id = 1L, total = "10.00", currency = "USD"),
                makeOrder(id = 2L, total = "480.00", currency = "USD"),
                makeOrder(id = 3L, total = "120.00", currency = "USD"),
            )
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.success(AIOrdersDataSource.OrdersPage(orders = orders, canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            val json = (result as ToolResult.Success).structured.jsonObject
            val totalRange = requireNotNull(json["total_range"]).jsonObject
            assertThat(requireNotNull(totalRange["min"]).jsonPrimitive.content).isEqualTo("10.00")
            assertThat(requireNotNull(totalRange["max"]).jsonPrimitive.content).isEqualTo("480.00")
            assertThat(requireNotNull(totalRange["currency"]).jsonPrimitive.content).isEqualTo("USD")
        }

    @Test
    fun `given empty result, when execute is called, then total_range is absent and count is zero`() =
        runTest {
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.success(AIOrdersDataSource.OrdersPage(orders = emptyList(), canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(requireNotNull(json["count"]).jsonPrimitive.int).isEqualTo(0)
            assertThat(json["total_range"]).isNull()
        }

    @Test
    fun `given data source fails, when execute is called, then a retryable TransportError is returned`() =
        runTest {
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.failure(IllegalStateException("network error"))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
            val error = result as ToolResult.TransportError
            assertThat(error.toolCallId).isEqualTo("call-1")
            assertThat(error.retryable).isTrue
        }

    @Test
    fun `given search argument is an integer, when execute is called, then a ValidationError is returned`() =
        runTest {
            val result = handler.execute(
                toolCall(buildJsonObject { put("search", 42) })
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            val error = result as ToolResult.ValidationError
            assertThat(error.toolCallId).isEqualTo("call-1")
        }
}
