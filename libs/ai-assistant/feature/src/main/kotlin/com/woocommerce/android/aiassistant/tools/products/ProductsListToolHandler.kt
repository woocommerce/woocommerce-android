package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.inputSchema
import com.woocommerce.android.aiassistant.core.chat.parseArgs
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.tools.validateAllowedArguments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject

internal class ProductsListToolHandler @Inject constructor(
    private val dataSource: AIProductsDataSource,
    @AiAssistantJson private val json: Json,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = "products_list",
        description = "List products, optionally filtered by status, or keyword search. " +
            "For aggregate sales / top sellers prefer the analytics tools. " +
            "For prose questions about a specific product's stock quantity, prices, etc., " +
            "call products_get with the ID.",
        inputSchema = inputSchema {
            string("search", description = "Free-text search across product name and content.")
            enum(
                "status",
                values = listOf("any", "draft", "pending", "private", "publish"),
                description = "Publication status; default 'any'.",
            )
            integer("category", description = "Category ID filter.")
            string("sku", description = "Exact SKU lookup.")
            array("include", itemType = "integer", description = "Product IDs to include.")
            enum(
                "stock_status",
                values = PRODUCT_STOCK_STATUSES.toList(),
                description = "Stock status filter.",
            )
            enum(
                "orderby",
                values = PRODUCT_ORDER_BY_VALUES.toList(),
                description = "Sort field. Supported values: date, title, popularity.",
            )
            enum(
                "order",
                values = PRODUCT_SORT_ORDERS.toList(),
                description = "Sort direction.",
            )
            integer("page", description = "1-based page number; default 1.")
            integer("per_page", description = "Max items; clamped 1-50, default 20.")
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        validateAllowedArguments(call.arguments, PRODUCTS_LIST_ALLOWED_ARGS, descriptor.name).exceptionOrNull()?.let {
            return ToolResult.ValidationError(call.id, it.message ?: "Invalid arguments")
        }
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        args.validate()?.let { message ->
            return ToolResult.ValidationError(call.id, message)
        }
        return dataSource.fetchProducts(
            search = args.search,
            status = args.status,
            page = args.page,
            perPage = args.perPage,
            category = args.category,
            sku = args.sku,
            include = args.include,
            stockStatus = args.stockStatus,
            orderby = args.orderby,
            order = args.order,
        ).fold(
            onSuccess = { page ->
                val products = page.products
                val statusCounts = products.groupingBy { it.status }.eachCount()
                val response = ProductListResponse(
                    count = products.size,
                    ids = products.map { it.remoteProductId },
                    products = products.map { it.toProductListRowResponse() },
                    canLoadMore = page.canLoadMore,
                    statusCounts = statusCounts,
                )
                ToolResult.Success(toolCallId = call.id, structured = json.encodeToJsonElement(response) as JsonObject)
            },
            onFailure = { ToolResult.TransportError(toolCallId = call.id, retryable = true) },
        )
    }

    @Serializable
    private data class Args(
        val search: String? = null,
        val status: String? = null,
        val category: Int? = null,
        val sku: String? = null,
        val include: List<Long>? = null,
        @SerialName("stock_status") val stockStatus: String? = null,
        val orderby: String? = null,
        val order: String? = null,
        val page: Int = 1,
        @SerialName("per_page") val perPage: Int = 20,
    ) {
        fun validate(): String? = listOfNotNull(
            invalidEnumMessage(status, PRODUCT_STATUSES, "status"),
            invalidEnumMessage(stockStatus, PRODUCT_STOCK_STATUSES, "stock_status"),
            invalidEnumMessage(orderby, PRODUCT_ORDER_BY_VALUES, "orderby"),
            invalidEnumMessage(order, PRODUCT_SORT_ORDERS, "order"),
            "include must contain at least one product ID.".takeIf { include != null && include.isEmpty() },
            "include can contain at most $MAX_INCLUDE_IDS product IDs.".takeIf {
                include != null && include.size > MAX_INCLUDE_IDS
            },
            "include cannot be combined with search, sku, orderby, or order.".takeIf { hasAmbiguousInclude() },
            "orderby and order cannot be combined with search or sku.".takeIf { hasSearchSortCombination() },
        ).firstOrNull()

        private fun hasAmbiguousInclude(): Boolean =
            !include.isNullOrEmpty() && (hasSearchQuery() || hasSkuQuery() || hasSort())

        private fun hasSearchSortCombination(): Boolean =
            (hasSearchQuery() || hasSkuQuery()) && hasSort()

        private fun hasSearchQuery(): Boolean = !search.isNullOrBlank()

        private fun hasSkuQuery(): Boolean = !sku.isNullOrBlank()

        private fun hasSort(): Boolean = orderby != null || order != null
    }

    @Serializable
    private data class ProductListResponse(
        val count: Int,
        val ids: List<Long>,
        val products: List<ProductListRowResponse>,
        @SerialName("can_load_more") val canLoadMore: Boolean,
        @SerialName("status_counts") val statusCounts: Map<String, Int>,
    )
}

private val PRODUCTS_LIST_ALLOWED_ARGS = setOf(
    "search",
    "status",
    "category",
    "sku",
    "include",
    "stock_status",
    "orderby",
    "order",
    "page",
    "per_page",
)

private val PRODUCT_STATUSES = setOf("any", "draft", "pending", "private", "publish")
private val PRODUCT_STOCK_STATUSES = setOf("instock", "outofstock", "onbackorder")
private val PRODUCT_ORDER_BY_VALUES = setOf("date", "title", "popularity")
private val PRODUCT_SORT_ORDERS = setOf("asc", "desc")
private const val MAX_INCLUDE_IDS = 100

private fun invalidEnumMessage(value: String?, allowed: Set<String>, name: String): String? =
    value?.takeUnless { it in allowed }?.let { "'$it' is not an allowed $name." }
