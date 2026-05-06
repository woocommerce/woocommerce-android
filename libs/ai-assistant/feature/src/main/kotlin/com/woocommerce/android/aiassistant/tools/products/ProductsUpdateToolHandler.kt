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

internal class ProductsUpdateToolHandler @Inject constructor(
    private val dataSource: AIProductsDataSource,
    @AiAssistantJson private val json: Json,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = "products_update",
        description = "Update a single simple product. Accepts only name, regular_price, sale_price, " +
            "stock_quantity, and status. Setting stock_quantity also enables stock management. " +
            "Variable products should be updated through individual variations, not the parent product. " +
            "At most one write is executed per turn.",
        inputSchema = inputSchema {
            integer("id", description = "The product ID. Required.", required = true)
            string("name", description = "New product name.")
            string("regular_price", description = "New regular price as a string.")
            string("sale_price", description = "New sale price as a string.")
            integer("stock_quantity", description = "New stock quantity. Also enables manage_stock.")
            enum(
                "status",
                values = listOf("draft", "pending", "private", "publish"),
                description = "New product status.",
            )
        },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        if (!args.hasUpdates()) {
            return ToolResult.ValidationError(call.id, "At least one product field must be provided.")
        }
        if (args.status != null && args.status !in ALLOWED_STATUSES) {
            return ToolResult.ValidationError(call.id, "'${args.status}' is not an allowed status.")
        }

        return dataSource.updateProduct(
            productId = args.id,
            update = AIProductsDataSource.ProductUpdate(
                name = args.name,
                regularPrice = args.regularPrice,
                salePrice = args.salePrice,
                stockQuantity = args.stockQuantity,
                status = args.status,
            ),
        ).fold(
            onSuccess = { product ->
                ToolResult.Success(
                    toolCallId = call.id,
                    structured = json.encodeToJsonElement(product.toProductDetailResponse()) as JsonObject,
                )
            },
            onFailure = { error ->
                when (error) {
                    is AIProductsDataSource.UnsupportedProductTypeException -> ToolResult.ValidationError(
                        toolCallId = call.id,
                        reason = requireNotNull(error.message),
                    )
                    else -> ToolResult.TransportError(toolCallId = call.id, retryable = true)
                }
            },
        )
    }

    @Serializable
    private data class Args(
        val id: Long,
        val name: String? = null,
        @SerialName("regular_price") val regularPrice: String? = null,
        @SerialName("sale_price") val salePrice: String? = null,
        @SerialName("stock_quantity") val stockQuantity: Int? = null,
        val status: String? = null,
    ) {
        fun hasUpdates(): Boolean =
            name != null ||
                regularPrice != null ||
                salePrice != null ||
                stockQuantity != null ||
                status != null
    }

    private companion object {
        val ALLOWED_STATUSES = setOf("draft", "pending", "private", "publish")
    }
}
