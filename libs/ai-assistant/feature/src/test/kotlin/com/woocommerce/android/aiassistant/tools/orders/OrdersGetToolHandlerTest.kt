package com.woocommerce.android.aiassistant.tools.orders

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
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
class OrdersGetToolHandlerTest {

    private val dataSource: AIOrdersDataSource = mock()
    private val handler = OrdersGetToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "orders_get", arguments = arguments)

    private fun makeOrder(): OrderEntity = OrderEntity(
        localSiteId = LocalId(1),
        orderId = 123L,
        number = "123",
        status = "processing",
        total = "45.00",
        currency = "USD",
        billingFirstName = "John",
        billingLastName = "Doe",
        billingEmail = "john@example.com",
        paymentMethodTitle = "Credit Card",
        dateCreated = "2024-01-15T00:00:00",
    )

    @Test
    fun `given a valid id, when execute is called, then structured JSON contains iOS-aligned fields`() =
        runTest {
            val order = makeOrder()
            whenever(dataSource.getOrder(123L)).thenReturn(Result.success(order))

            val result = handler.execute(toolCall(buildJsonObject { put("id", 123) }))

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val success = result as ToolResult.Success
            assertThat(success.toolCallId).isEqualTo("call-1")

            val json = success.structured.jsonObject
            assertThat(requireNotNull(json["id"]).jsonPrimitive.int).isEqualTo(123)
            assertThat(requireNotNull(json["number"]).jsonPrimitive.content).isEqualTo("123")
            assertThat(requireNotNull(json["status"]).jsonPrimitive.content).isEqualTo("processing")
            assertThat(requireNotNull(json["total"]).jsonPrimitive.content).isEqualTo("45.00")
            assertThat(requireNotNull(json["currency"]).jsonPrimitive.content).isEqualTo("USD")
            assertThat(requireNotNull(json["customer_name"]).jsonPrimitive.content).isEqualTo("John Doe")
            assertThat(requireNotNull(json["customer_email"]).jsonPrimitive.content).isEqualTo("john@example.com")
            assertThat(requireNotNull(json["date_created"]).jsonPrimitive.content).isEqualTo("2024-01-15T00:00:00")
            assertThat(requireNotNull(json["payment_method_title"]).jsonPrimitive.content).isEqualTo("Credit Card")
        }

    @Test
    fun `given missing id, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(toolCall(buildJsonObject {}))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            val error = result as ToolResult.ValidationError
            assertThat(error.toolCallId).isEqualTo("call-1")
            assertThat(error.reason).isNotBlank()
        }

    @Test
    fun `given non-integer id, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(toolCall(buildJsonObject { put("id", "abc") }))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            val error = result as ToolResult.ValidationError
            assertThat(error.toolCallId).isEqualTo("call-1")
        }

    @Test
    fun `given the data source fails, when execute is called, then retryable TransportError is returned`() =
        runTest {
            whenever(dataSource.getOrder(7L)).thenReturn(Result.failure(IllegalStateException("network error")))

            val result = handler.execute(toolCall(buildJsonObject { put("id", 7) }))

            assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
            val error = result as ToolResult.TransportError
            assertThat(error.toolCallId).isEqualTo("call-1")
            assertThat(error.retryable).isTrue
        }
}
