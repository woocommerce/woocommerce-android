package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.inputSchema
import com.woocommerce.android.aiassistant.core.chat.parseArgs
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.tools.ToolFailureDiagnosticsFactory
import com.woocommerce.android.aiassistant.tools.validateAllowedArguments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject

internal class ProductVariationsUpdateToolHandler @Inject constructor(
    private val dataSource: AIProductVariationsDataSource,
    @AiAssistantJson private val json: Json,
    private val diagnosticsFactory: ToolFailureDiagnosticsFactory,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = "product_variations_update",
        description = "Update one product variation. Requires parent product_id and variation id. " +
            "Accepts only regular_price, sale_price, stock_quantity, stock_status, sku, and status. " +
            "Setting stock_quantity also enables stock management. At most one write is executed per turn.",
        inputSchema = inputSchema {
            integer("product_id", description = "The parent product ID. Required.", required = true)
            integer("id", description = "The variation ID. Required.", required = true)
            string("regular_price", description = "New regular price as a string.")
            string("sale_price", description = "New sale price as a string.")
            integer("stock_quantity", description = "New stock quantity. Also enables manage_stock.")
            enum(
                "stock_status",
                values = listOf("instock", "outofstock", "onbackorder"),
                description = "New stock status.",
            )
            string("sku", description = "New variation SKU.")
            enum(
                "status",
                values = listOf("draft", "pending", "private", "publish"),
                description = "New variation status.",
            )
        },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        validateAllowedArguments(call.arguments, PRODUCT_VARIATIONS_UPDATE_ALLOWED_ARGS, descriptor.name)
            .exceptionOrNull()?.let {
                return ToolResult.ValidationError(call.id, it.message ?: "Invalid arguments")
            }
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        args.validationError()?.let {
            return ToolResult.ValidationError(call.id, it)
        }

        return dataSource.updateVariation(
            productId = args.productId,
            variationId = args.id,
            update = AIProductVariationsDataSource.VariationUpdate(
                regularPrice = args.regularPrice,
                salePrice = args.salePrice,
                stockQuantity = args.stockQuantity,
                stockStatus = args.stockStatus,
                sku = args.sku,
                status = args.status,
            ),
        ).fold(
            onSuccess = { variation ->
                ToolResult.Success(
                    toolCallId = call.id,
                    structured = json.encodeToJsonElement(
                        variation.toProductVariationDetailResponse()
                    ) as JsonObject,
                )
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
        @SerialName("product_id") val productId: Long,
        val id: Long,
        @SerialName("regular_price") val regularPrice: String? = null,
        @SerialName("sale_price") val salePrice: String? = null,
        @SerialName("stock_quantity") val stockQuantity: Int? = null,
        @SerialName("stock_status") val stockStatus: String? = null,
        val sku: String? = null,
        val status: String? = null,
    ) {
        fun hasUpdates(): Boolean =
            regularPrice != null ||
                salePrice != null ||
                stockQuantity != null ||
                stockStatus != null ||
                sku != null ||
                status != null

        fun validationError(): String? =
            when {
                !hasUpdates() -> "At least one variation field must be provided."
                stockStatus != null && stockStatus !in ALLOWED_STOCK_STATUSES ->
                    "'$stockStatus' is not an allowed stock_status."
                status != null && status !in ALLOWED_STATUSES -> "'$status' is not an allowed status."
                else -> null
            }
    }

    private companion object {
        val ALLOWED_STOCK_STATUSES = setOf("instock", "outofstock", "onbackorder")
        val ALLOWED_STATUSES = setOf("draft", "pending", "private", "publish")
    }
}

private val PRODUCT_VARIATIONS_UPDATE_ALLOWED_ARGS = setOf(
    "product_id",
    "id",
    "regular_price",
    "sale_price",
    "stock_quantity",
    "stock_status",
    "sku",
    "status",
)
