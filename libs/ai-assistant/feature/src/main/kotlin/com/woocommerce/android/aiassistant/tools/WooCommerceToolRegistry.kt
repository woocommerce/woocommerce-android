package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.buildJsonObject

class WooCommerceToolRegistry(
    private val handlers: Map<String, AssistantToolHandler>,
) : ToolRegistry {

    override fun descriptors(): List<ToolDescriptor> = CATALOG

    override suspend fun execute(call: ToolCall): ToolResult {
        val handler = handlers[call.name]
            ?: return ToolResult.ValidationError(call.id, "No handler registered for tool: ${call.name}")
        return handler.execute(call)
    }

    companion object {
        val CATALOG: List<ToolDescriptor> = listOf(
            ToolDescriptor(
                name = "orders_list",
                description = "List and search orders. Supports filtering by status, date range, and customer.",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.SAFE,
            ),
            ToolDescriptor(
                name = "orders_get",
                description = "Get a single order by ID.",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.SAFE,
            ),
            ToolDescriptor(
                name = "orders_update",
                description = """
                    Update a single order. Accepts only the `status` field.
                    Allowed transitions: on-hold -> processing, processing -> completed.
                    Cancellation and refund flows are not supported by this tool.
                    At most one write is executed per turn; additional write calls in the same turn will be rejected by the runtime.
                """.trimIndent(),
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.UNSAFE,
            ),
            ToolDescriptor(
                name = "orders_bulk_update",
                description = """
                    Update multiple orders. Accepts only the `status` field per order.
                    Allowed transitions: on-hold -> processing, processing -> completed.
                    Bulk writes require confirmation. At most one write operation (single or bulk) is executed per turn.
                """.trimIndent(),
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.UNSAFE,
            ),
            ToolDescriptor(
                name = "products_list",
                description = "List and search products. Supports filtering by status, category, and stock.",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.SAFE,
            ),
            ToolDescriptor(
                name = "products_get",
                description = "Get a single product by ID.",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.SAFE,
            ),
            ToolDescriptor(
                name = "products_update",
                description = """
                    Update a single product. Accepts only these fields: regular_price, manage_stock, stock_quantity, stock_status, status.
                    At most one write is executed per turn; additional write calls in the same turn will be rejected by the runtime.
                """.trimIndent(),
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.UNSAFE,
            ),
            ToolDescriptor(
                name = "products_bulk_update",
                description = """
                    Update multiple products. Accepts only these fields per product: regular_price, manage_stock, stock_quantity, stock_status, status.
                    Bulk writes require confirmation. At most one write operation (single or bulk) is executed per turn.
                """.trimIndent(),
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.UNSAFE,
            ),
            ToolDescriptor(
                name = "product_variations_list",
                description = "List variations for a variable product.",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.SAFE,
            ),
            ToolDescriptor(
                name = "analytics_revenue",
                description = "Get revenue analytics for a date range.",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.SAFE,
            ),
            ToolDescriptor(
                name = "analytics_orders",
                description = "Get order-count analytics for a date range.",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.SAFE,
            ),
            ToolDescriptor(
                name = "show_cards",
                description = "Show entity cards in the UI for orders or products selected by the assistant.",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.SAFE,
            ),
            ToolDescriptor(
                name = "customers_list",
                description = "List customers or get a specific customer by ID.",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.SAFE,
            ),
        )
    }
}
