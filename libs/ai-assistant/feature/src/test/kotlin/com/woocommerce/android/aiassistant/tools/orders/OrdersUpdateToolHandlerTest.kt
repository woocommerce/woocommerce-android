package com.woocommerce.android.aiassistant.tools.orders

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolFailureKind
import com.woocommerce.android.aiassistant.core.chat.ToolResult
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersUpdateToolHandlerTest {

    private val dataSource: AIOrdersDataSource = mock()
    private val handler = OrdersUpdateToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "orders_update", arguments = arguments)

    @Test
    fun `given missing id, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(toolCall(buildJsonObject { put("status", "processing") }))

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }

    @Test
    fun `given missing status, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(toolCall(buildJsonObject { put("id", 123) }))

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }

    @Test
    fun `given status not in allowed set, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("id", 123)
                    put("status", "trash")
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }

    @Test
    fun `given valid status, when execute is called, then updateOrderStatus is called and success is returned`() =
        runTest {
            whenever(dataSource.updateOrderStatus(123L, "processing")).thenReturn(Result.success(Unit))
            whenever(dataSource.getOrder(123L)).thenReturn(Result.success(makeOrder(status = "processing")))

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("id", 123)
                        put("status", "processing")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(requireNotNull(json["id"]).jsonPrimitive.long).isEqualTo(123L)
            assertThat(requireNotNull(json["status"]).jsonPrimitive.content).isEqualTo("processing")
            verify(dataSource).updateOrderStatus(123L, "processing")
        }

    @Test
    fun `given update succeeds, when execute is called, then widened updated order detail is returned`() = runTest {
        val updated = makeOrder(
            status = "completed",
            lineItems = """[{"id":1,"name":"Socks","quantity":1,"sku":"SOCK","total":"12.00","product_id":7,"variation_id":0}]""",
        )
        whenever(dataSource.updateOrderStatus(123L, "completed")).thenReturn(Result.success(Unit))
        whenever(dataSource.getOrder(123L)).thenReturn(Result.success(updated))

        val result = handler.execute(toolCall(buildJsonObject { put("id", 123); put("status", "completed") }))

        val json = (result as ToolResult.Success).structured.jsonObject
        assertThat(json.keys).doesNotContain("order_id")
        assertThat(json.getValue("id").jsonPrimitive.long).isEqualTo(123L)
        assertThat(json.getValue("status").jsonPrimitive.content).isEqualTo("completed")
        assertThat(json.getValue("line_items").jsonArray.single().jsonObject.getValue("name").jsonPrimitive.content)
            .isEqualTo("Socks")
    }

    @Test
    fun `given unknown arg, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(buildJsonObject { put("id", 123); put("status", "completed"); put("extra_fields", "billing") })
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }

    @Test
    fun `given update succeeds but follow-up fetch fails, when execute is called, then success fallback is returned`() =
        runTest {
            whenever(dataSource.updateOrderStatus(123L, "completed")).thenReturn(Result.success(Unit))
            whenever(dataSource.getOrder(123L)).thenReturn(Result.failure(RuntimeException("fetch failed")))

            val result = handler.execute(toolCall(buildJsonObject { put("id", 123); put("status", "completed") }))

            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(json.getValue("order_id").jsonPrimitive.long).isEqualTo(123L)
            assertThat(json.getValue("status").jsonPrimitive.content).isEqualTo("completed")
            assertThat(json.getValue("response_partial").jsonPrimitive.boolean).isTrue
            assertThat(json.getValue("warning").jsonPrimitive.content)
                .isEqualTo("Order status updated, but updated order details could not be fetched.")
        }

    @Test
    fun `given invalid order id failure, when execute is called, then invalid id TransportError is returned`() =
        runTest {
            whenever(dataSource.updateOrderStatus(123L, "processing")).thenReturn(
                Result.failure(orderFailure(WCOrderStore.OrderErrorType.INVALID_ID))
            )

            val result = executeValidUpdate()

            assertTransportErrorKind(result, ToolFailureKind.DETERMINISTIC_FAILURE)
        }

    @Test
    fun `given local missing order failure, when execute is called, then not found TransportError is returned`() =
        runTest {
            whenever(dataSource.updateOrderStatus(123L, "processing")).thenReturn(
                Result.failure(
                    OnChangedException(WCOrderStore.OrderError(message = "Order with id 123 not found"))
                )
            )

            val result = executeValidUpdate()

            assertTransportErrorKind(result, ToolFailureKind.DETERMINISTIC_FAILURE)
        }

    @Test
    fun `given validation order failure, when execute is called, then validation TransportError is returned`() =
        runTest {
            whenever(dataSource.updateOrderStatus(123L, "processing")).thenReturn(
                Result.failure(orderFailure(WCOrderStore.OrderErrorType.ORDER_STATUS_NOT_FOUND))
            )

            val result = executeValidUpdate()

            assertTransportErrorKind(result, ToolFailureKind.DETERMINISTIC_FAILURE)
        }

    @Test
    fun `given generic update failure, when execute is called, then outcome unknown TransportError is returned`() =
        runTest {
            whenever(dataSource.updateOrderStatus(123L, "processing")).thenReturn(
                Result.failure(RuntimeException("network error"))
            )

            val result = executeValidUpdate()

            assertTransportErrorKind(result, ToolFailureKind.OUTCOME_UNKNOWN)
        }

    private suspend fun executeValidUpdate(): ToolResult =
        handler.execute(
            toolCall(
                buildJsonObject {
                    put("id", 123)
                    put("status", "processing")
                }
            )
        )

    private fun orderFailure(type: WCOrderStore.OrderErrorType): OnChangedException =
        OnChangedException(WCOrderStore.OrderError(type = type))

    private fun assertTransportErrorKind(result: ToolResult, kind: ToolFailureKind) {
        assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
        val error = result as ToolResult.TransportError
        assertThat(error.retryable).isTrue
        assertThat(error.kind).isEqualTo(kind)
    }

    private fun makeOrder(
        status: String = "processing",
        lineItems: String = "[]",
    ) = OrderEntity(
        localSiteId = LocalId(1),
        orderId = 123L,
        number = "123",
        status = status,
        total = "45.00",
        currency = "USD",
        dateCreated = "2026-05-01T10:00:00Z",
        dateModified = "2026-05-01T11:00:00Z",
        customerId = 55L,
        billingFirstName = "Jane",
        billingLastName = "Doe",
        lineItems = lineItems,
    )
}
