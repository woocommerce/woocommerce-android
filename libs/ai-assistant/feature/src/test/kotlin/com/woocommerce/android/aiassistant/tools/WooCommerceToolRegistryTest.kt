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
    fun `when CATALOG is inspected, then all 13 expected tool names are present`() {
        val names = WooCommerceToolRegistry.CATALOG.map { it.name }

        assertThat(names).containsExactlyInAnyOrder(
            "orders_list",
            "orders_get",
            "orders_update",
            "orders_bulk_update",
            "products_list",
            "products_get",
            "products_update",
            "products_bulk_update",
            "product_variations_list",
            "analytics_revenue",
            "analytics_orders",
            "show_cards",
            "customers_list",
        )
    }

    @Test
    fun `given no registered handlers, when descriptors are retrieved, then empty list is returned`() {
        val registry = WooCommerceToolRegistry(emptyMap())

        assertThat(registry.descriptors()).isEmpty()
    }

    @Test
    fun `given handlers registered for a subset of tools, when descriptors are retrieved, then only those tools are returned`() {
        val registry = WooCommerceToolRegistry(
            mapOf(
                "orders_list" to FakeToolHandler(fakeDescriptor("orders_list")) { ToolResult.Success(it.id, buildJsonObject { }) },
                "products_get" to FakeToolHandler(fakeDescriptor("products_get")) { ToolResult.Success(it.id, buildJsonObject { }) },
            )
        )

        val names = registry.descriptors().map { it.name }

        assertThat(names).containsExactlyInAnyOrder("orders_list", "products_get")
    }

    @Test
    fun `when CATALOG is inspected, then write tools are UNSAFE and read tools are SAFE`() {
        val descriptorsByName = WooCommerceToolRegistry.CATALOG.associateBy { it.name }

        val writeToolNames = setOf("orders_update", "orders_bulk_update", "products_update", "products_bulk_update")
        writeToolNames.forEach { name ->
            assertThat(descriptorsByName.getValue(name).safetyLevel)
                .`as`("$name should be UNSAFE")
                .isEqualTo(ToolSafetyLevel.UNSAFE)
        }

        val readToolNames = descriptorsByName.keys - writeToolNames
        readToolNames.forEach { name ->
            assertThat(descriptorsByName.getValue(name).safetyLevel)
                .`as`("$name should be SAFE")
                .isEqualTo(ToolSafetyLevel.SAFE)
        }
    }

    @Test
    fun `when descriptors are retrieved, then write-tool descriptions include allowlisted fields and constraints`() {
        val descriptorsByName = WooCommerceToolRegistry.CATALOG.associateBy { it.name }

        val ordersUpdate = requireNotNull(descriptorsByName["orders_update"]).description
        assertThat(ordersUpdate).contains("status")
        assertThat(ordersUpdate).contains("on-hold")

        val ordersBulkUpdate = requireNotNull(descriptorsByName["orders_bulk_update"]).description
        assertThat(ordersBulkUpdate).contains("status")
        assertThat(ordersBulkUpdate).contains("Bulk writes require confirmation")

        val productsUpdate = requireNotNull(descriptorsByName["products_update"]).description
        assertThat(productsUpdate).contains("regular_price")
        assertThat(productsUpdate).contains("stock_quantity")

        val productsBulkUpdate = requireNotNull(descriptorsByName["products_bulk_update"]).description
        assertThat(productsBulkUpdate).contains("regular_price")
        assertThat(productsBulkUpdate).contains("Bulk writes require confirmation")
    }

    @Test
    fun `given empty handlers map, when execute is called with unknown tool name, then ValidationError is returned`() =
        runTest {
            val registry = WooCommerceToolRegistry(emptyMap())

            val result = registry.execute(ToolCall(id = "c1", name = "nonexistent", arguments = buildJsonObject { }))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            val error = result as ToolResult.ValidationError
            assertThat(error.reason).contains("nonexistent")
        }

    @Test
    fun `given a registered handler, when execute is called with matching tool name, then handler result is returned`() =
        runTest {
            val expectedResult = ToolResult.Success(
                toolCallId = "call_1",
                structured = buildJsonObject { },
            )
            val handler = FakeToolHandler(fakeDescriptor("orders_list")) { expectedResult }
            val registry = WooCommerceToolRegistry(mapOf("orders_list" to handler))

            val result = registry.execute(
                ToolCall(id = "call_1", name = "orders_list", arguments = buildJsonObject { })
            )

            assertThat(result).isEqualTo(expectedResult)
        }

    @Test
    fun `given empty handlers map, when execute is called with known catalog tool name, then ValidationError is returned`() =
        runTest {
            val registry = WooCommerceToolRegistry(emptyMap())

            val result = registry.execute(ToolCall(id = "c2", name = "orders_list", arguments = buildJsonObject { }))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        }

    private class FakeToolHandler(
        override val descriptor: ToolDescriptor,
        private val onExecute: suspend (ToolCall) -> ToolResult,
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
