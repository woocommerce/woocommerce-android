package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooCommerceToolRegistryTest {

    @Test
    fun `given an empty handler set, when descriptors are retrieved, then empty list is returned`() {
        val registry = WooCommerceToolRegistry(emptySet())

        assertThat(registry.descriptors()).isEmpty()
    }

    @Test
    fun `given handlers, when descriptors are retrieved, then descriptors from each handler are exposed`() {
        val ordersList = FakeToolHandler(fakeDescriptor("orders_list"))
        val productsGet = FakeToolHandler(fakeDescriptor("products_get"))
        val registry = WooCommerceToolRegistry(setOf(ordersList, productsGet))

        val names = registry.descriptors().map { it.name }

        assertThat(names).containsExactlyInAnyOrder("orders_list", "products_get")
    }

    @Test
    fun `given an empty handler set, when execute is called with any tool name, then ValidationError is returned`() =
        runTest {
            val registry = WooCommerceToolRegistry(emptySet())

            val result = registry.execute(ToolCall(id = "c1", name = "nonexistent", arguments = buildJsonObject { }))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            val error = result as ToolResult.ValidationError
            assertThat(error.reason).contains("nonexistent")
        }

    @Test
    fun `given a registered handler, when execute is called with matching tool name, then handler result is returned`() =
        runTest {
            val expected = ToolResult.Success(toolCallId = "call_1", structured = buildJsonObject { })
            val handler = FakeToolHandler(fakeDescriptor("orders_list")) { expected }
            val registry = WooCommerceToolRegistry(setOf(handler))

            val result = registry.execute(
                ToolCall(id = "call_1", name = "orders_list", arguments = buildJsonObject { })
            )

            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `given a registered handler, when execute is called with a different tool name, then ValidationError is returned`() =
        runTest {
            val handler = FakeToolHandler(fakeDescriptor("orders_list"))
            val registry = WooCommerceToolRegistry(setOf(handler))

            val result = registry.execute(
                ToolCall(id = "call_2", name = "products_get", arguments = buildJsonObject { })
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        }

    private class FakeToolHandler(
        override val descriptor: ToolDescriptor,
        private val onExecute: suspend (ToolCall) -> ToolResult = { ToolResult.Success(it.id, buildJsonObject { }) },
    ) : AssistantToolHandler {
        override suspend fun execute(call: ToolCall): ToolResult = onExecute(call)
    }

    private fun fakeDescriptor(name: String) = ToolDescriptor(
        name = name,
        description = "fake",
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.SAFE,
    )
}
