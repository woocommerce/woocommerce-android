package com.woocommerce.android.aiassistant.tools.orders

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.inputSchema
import com.woocommerce.android.aiassistant.core.chat.parseArgs
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.tools.RestDateBounds
import com.woocommerce.android.aiassistant.tools.ToolFailureDiagnosticsFactory
import com.woocommerce.android.aiassistant.tools.validateAllowedArguments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject

internal class OrdersListToolHandler @Inject constructor(
    private val dataSource: AIOrdersDataSource,
    @AiAssistantJson private val json: Json,
    private val diagnosticsFactory: ToolFailureDiagnosticsFactory,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = "orders_list",
        description = "List orders, optionally filtered by status, date range, or customer. Use to find " +
            "specific orders, list pending fulfilment, or pull the most recent N. For aggregate sales, revenue, " +
            "order count, or average order value numbers prefer analytics_orders. For prose questions about " +
            "a specific " +
            "order's payment method, customer email, etc., call orders_get with the ID.",
        inputSchema = inputSchema {
            enum(
                "status",
                values = listOf(
                    "any", "pending", "processing", "on-hold", "completed",
                    "cancelled", "refunded", "failed", "trash",
                ),
                description = "Order status filter; use 'any' to skip filtering.",
            )
            string("search", description = "Free-text search across order content.")
            integer("customer", description = "Customer ID; resolve via customers_list first.")
            array("include", itemType = "integer", description = "Specific order IDs to include.")
            string("after", description = "YYYY-MM-DD lower bound on date_created.")
            string("before", description = "YYYY-MM-DD upper bound on date_created.")
            enum(
                "orderby",
                values = listOf("date", "id", "modified", "title"),
                description = "Sort key; default 'date'.",
            )
            enum("order", values = listOf("asc", "desc"), description = "Sort direction; default 'desc'.")
            integer("page", description = "1-based page number; default 1.")
            integer("per_page", description = "Max items; clamped 1-50, default 20.")
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    @Suppress("ReturnCount")
    override suspend fun execute(call: ToolCall): ToolResult {
        validateAllowedArguments(call.arguments, ORDERS_LIST_ALLOWED_ARGS, descriptor.name).exceptionOrNull()?.let {
            return ToolResult.ValidationError(call.id, it.message ?: "Invalid arguments")
        }
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        val after = args.after?.let {
            RestDateBounds.lowerBound(it)
                ?: return ToolResult.ValidationError(call.id, "after must be YYYY-MM-DD")
        }
        val before = args.before?.let {
            RestDateBounds.upperBound(it)
                ?: return ToolResult.ValidationError(call.id, "before must be YYYY-MM-DD")
        }
        return dataSource.fetchOrders(
            search = args.search,
            status = args.status,
            page = args.page,
            perPage = args.perPage,
            customer = args.customer,
            include = args.include,
            after = after,
            before = before,
            orderby = args.orderby,
            order = args.order,
        ).fold(
            onSuccess = { page ->
                val orders = page.orders
                val statusCounts = orders.groupingBy { it.status }.eachCount()
                val totalRange = orders.takeIf { it.isNotEmpty() }?.let { nonEmpty ->
                    val totals = nonEmpty.map { it.total.toBigDecimal() }
                    TotalRange(
                        min = requireNotNull(totals.minOrNull()).toPlainString(),
                        max = requireNotNull(totals.maxOrNull()).toPlainString(),
                        currency = nonEmpty.first().currency,
                    )
                }
                val response = OrderListResponse(
                    count = orders.size,
                    ids = orders.map { it.orderId },
                    orders = orders.map { it.toOrderListRowResponse() },
                    statusCounts = statusCounts,
                    totalRange = totalRange,
                )
                ToolResult.Success(toolCallId = call.id, structured = json.encodeToJsonElement(response) as JsonObject)
            },
            onFailure = { error ->
                diagnosticsFactory.transportError(
                    toolCallId = call.id,
                    toolName = descriptor.name,
                    error = error,
                    retryable = true,
                )
            },
        )
    }

    @Serializable
    private data class Args(
        val search: String? = null,
        val status: String? = null,
        val customer: Long? = null,
        val include: List<Long>? = null,
        val after: String? = null,
        val before: String? = null,
        val orderby: String? = null,
        val order: String? = null,
        val page: Int = 1,
        @SerialName("per_page") val perPage: Int = 20,
    )

    @Serializable
    private data class TotalRange(val min: String, val max: String, val currency: String)

    @Serializable
    private data class OrderListResponse(
        val count: Int,
        val ids: List<Long>,
        val orders: List<OrderListRowResponse>,
        @SerialName("status_counts") val statusCounts: Map<String, Int>,
        @SerialName("total_range") val totalRange: TotalRange?,
    )
}

private val ORDERS_LIST_ALLOWED_ARGS = setOf(
    "status",
    "search",
    "customer",
    "include",
    "after",
    "before",
    "orderby",
    "order",
    "page",
    "per_page",
)
