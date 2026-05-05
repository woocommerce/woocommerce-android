package com.woocommerce.android.aiassistant.tools.orders

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
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
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrdersStatusResult.FailedOrder

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersBulkUpdateToolHandlerTest {

    private val dataSource: AIOrdersDataSource = mock()
    private val handler = OrdersBulkUpdateToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "orders_bulk_update", arguments = arguments)

    @Test
    fun `given descriptor, when inspected, then schema matches bulk update contract`() {
        val schema = handler.descriptor.inputSchema
        val properties = requireNotNull(schema["properties"]).jsonObject
        val ids = requireNotNull(properties["ids"]).jsonObject
        val patch = requireNotNull(properties["patch"]).jsonObject
        val patchProperties = requireNotNull(patch["properties"]).jsonObject

        assertThat(handler.descriptor.name).isEqualTo("orders_bulk_update")
        assertThat(handler.descriptor.safetyLevel).isEqualTo(ToolSafetyLevel.UNSAFE)
        assertThat(requireNotNull(schema["additionalProperties"]).jsonPrimitive.content).isEqualTo("false")
        assertThat(requireNotNull(schema["required"]).jsonArray.map { it.jsonPrimitive.content })
            .containsExactly("ids", "patch")
        assertThat(requireNotNull(ids["items"]).jsonObject["type"]?.jsonPrimitive?.content).isEqualTo("integer")
        assertThat(requireNotNull(ids["minItems"]).jsonPrimitive.content).isEqualTo("1")
        assertThat(requireNotNull(ids["maxItems"]).jsonPrimitive.content).isEqualTo("100")
        assertThat(requireNotNull(patch["additionalProperties"]).jsonPrimitive.content).isEqualTo("false")
        assertThat(requireNotNull(patch["minProperties"]).jsonPrimitive.content).isEqualTo("1")
        assertThat(patchProperties.keys).containsExactlyInAnyOrder("status", "customer_note", "billing_email")
    }

    @Test
    fun `given empty ids, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", JsonArray(emptyList()))
                    put("patch", buildJsonObject { put("status", "processing") })
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).bulkUpdateOrders(any(), any())
    }

    @Test
    fun `given more than 100 ids, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", buildJsonArray { repeat(101) { add(it + 1) } })
                    put("patch", buildJsonObject { put("status", "processing") })
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).bulkUpdateOrders(any(), any())
    }

    @Test
    fun `given empty patch, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", buildJsonArray { add(123) })
                    put("patch", buildJsonObject { })
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).bulkUpdateOrders(any(), any())
    }

    @Test
    fun `given unsupported status, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", buildJsonArray { add(123) })
                    put("patch", buildJsonObject { put("status", "trash") })
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }

    @Test
    fun `given valid patch, when execute succeeds, then structured summary is returned`() = runTest {
        whenever(
            dataSource.bulkUpdateOrders(
                orderIds = listOf(123L, 456L),
                patch = AIOrdersDataSource.OrderPatch(
                    status = "processing",
                    customerNote = "Please call first",
                    billingEmail = "customer@example.com",
                )
            )
        ).thenReturn(
            Result.success(
                AIOrdersDataSource.BulkUpdateResult(
                    updatedIds = listOf(123L),
                    failedOrders = listOf(
                        FailedOrder(
                            id = 456L,
                            errorCode = "woocommerce_rest_shop_order_invalid_id",
                            errorMessage = "Invalid ID.",
                            errorStatus = 400,
                        )
                    )
                )
            )
        )

        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put(
                        "ids",
                        buildJsonArray {
                            add(123)
                            add(456)
                        }
                    )
                    put(
                        "patch",
                        buildJsonObject {
                            put("status", "processing")
                            put("customer_note", "Please call first")
                            put("billing_email", "customer@example.com")
                        }
                    )
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.Success::class.java)
        val structured = (result as ToolResult.Success).structured.jsonObject
        assertThat(requireNotNull(structured["tool"]).jsonPrimitive.content).isEqualTo("orders_bulk_update")
        assertThat(requireNotNull(structured["updated_count"]).jsonPrimitive.content).isEqualTo("1")
        assertThat(requireNotNull(structured["failed_count"]).jsonPrimitive.content).isEqualTo("1")
        assertThat(requireNotNull(structured["updated_ids"]).jsonArray.single().jsonPrimitive.long).isEqualTo(123L)
        val failed = requireNotNull(structured["failed"]).jsonArray.single().jsonObject
        assertThat(requireNotNull(failed["id"]).jsonPrimitive.long).isEqualTo(456L)
        assertThat(requireNotNull(failed["code"]).jsonPrimitive.content)
            .isEqualTo("woocommerce_rest_shop_order_invalid_id")
        assertThat(requireNotNull(failed["message"]).jsonPrimitive.content).isEqualTo("Invalid ID.")
        assertThat(requireNotNull(failed["status"]).jsonPrimitive.content).isEqualTo("400")
    }

    @Test
    fun `given update fails, when execute is called, then retryable TransportError is returned`() = runTest {
        whenever(
            dataSource.bulkUpdateOrders(
                orderIds = listOf(123L),
                patch = AIOrdersDataSource.OrderPatch(status = "processing")
            )
        ).thenReturn(Result.failure(RuntimeException("network error")))

        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", buildJsonArray { add(123) })
                    put("patch", buildJsonObject { put("status", "processing") })
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
        assertThat((result as ToolResult.TransportError).retryable).isTrue
    }
}
