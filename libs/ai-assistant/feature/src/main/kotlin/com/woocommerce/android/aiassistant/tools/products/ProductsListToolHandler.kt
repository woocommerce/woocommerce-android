package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.inputSchema
import com.woocommerce.android.aiassistant.core.chat.parseArgs
import com.woocommerce.android.aiassistant.di.AiAssistantJson
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
        description = "List products, optionally filtered by a text search term. Use to find specific " +
            "products, check stock, or browse by name or SKU. For aggregate sales numbers prefer " +
            "analytics_revenue. For full product detail on a specific product use products_get with the ID.",
        inputSchema = inputSchema {
            string("search", description = "Free-text search across product name and SKU.")
            integer("page", description = "1-based page number; default 1.")
            integer("per_page", description = "Max items; clamped 1-50, default 20.")
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        return dataSource.fetchProducts(
            search = args.search,
            page = args.page,
            perPage = args.perPage,
        ).fold(
            onSuccess = { page ->
                val products = page.products
                val statusCounts = products.groupingBy { it.status }.eachCount()
                val response = ProductListResponse(
                    count = products.size,
                    ids = products.map { it.remoteProductId },
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
        val page: Int = 1,
        @SerialName("per_page") val perPage: Int = 20,
    )

    @Serializable
    private data class ProductListResponse(
        val count: Int,
        val ids: List<Long>,
        @SerialName("status_counts") val statusCounts: Map<String, Int>,
    )
}
