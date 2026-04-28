package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooCommerceToolRegistryTest {

    @Test
    fun `when descriptors are retrieved, then all 13 expected tool names are present`() {
        val registry = WooCommerceToolRegistry(emptyMap())

        val names = registry.descriptors().map { it.name }

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
    fun `when descriptors are retrieved, then write tools are UNSAFE and read tools are SAFE`() {
        val registry = WooCommerceToolRegistry(emptyMap())
        val descriptorsByName = registry.descriptors().associateBy { it.name }

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
    fun `when descriptors are retrieved, then write-tool descriptions include allowlisted fields`() {
        val registry = WooCommerceToolRegistry(emptyMap())
        val descriptorsByName = registry.descriptors().associateBy { it.name }

        assertThat(descriptorsByName.getValue("products_update").description).contains("regular_price")
        assertThat(descriptorsByName.getValue("orders_update").description).contains("on-hold")
    }

    @Test
    fun `given empty handlers map, when execute is called with unknown tool name, then ValidationError is returned`() =
        runTest {
            val registry = WooCommerceToolRegistry(emptyMap())

            val result = registry.execute(ToolCall(id = "c1", name = "nonexistent", arguments = buildJsonObject { }))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        }

    @Test
    fun `given a registered handler, when execute is called with matching tool name, then handler result is returned`() =
        runTest {
            val expectedResult = ToolResult.ValidationError("call_1", "fixed-response")
            val handler = AssistantToolHandler { expectedResult }
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
}
