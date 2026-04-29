package com.woocommerce.android.aiassistant.tools.orders

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
            assertThat(requireNotNull(json["order_id"]).jsonPrimitive.long).isEqualTo(123L)
            assertThat(requireNotNull(json["status"]).jsonPrimitive.content).isEqualTo("processing")
            verify(dataSource).updateOrderStatus(123L, "processing")
        }

    @Test
    fun `given cancelled status, when execute is called, then updateOrderStatus is called and success is returned`() =
        runTest {
            whenever(dataSource.updateOrderStatus(123L, "cancelled")).thenReturn(Result.success(Unit))

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("id", 123)
                        put("status", "cancelled")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(requireNotNull(json["status"]).jsonPrimitive.content).isEqualTo("cancelled")
            verify(dataSource).updateOrderStatus(123L, "cancelled")
        }

    @Test
    fun `given update fails, when execute is called, then retryable TransportError is returned`() = runTest {
        whenever(dataSource.updateOrderStatus(123L, "processing")).thenReturn(
            Result.failure(RuntimeException("network error"))
        )

        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("id", 123)
                    put("status", "processing")
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
        assertThat((result as ToolResult.TransportError).retryable).isTrue
    }
}
