package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.inputSchema
import com.woocommerce.android.aiassistant.core.chat.parseArgs
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.tools.parseExtraFields
import com.woocommerce.android.aiassistant.tools.validateAllowedArguments
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject

internal class ProductsGetToolHandler @Inject constructor(
    private val dataSource: AIProductsDataSource,
    @AiAssistantJson private val json: Json,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = "products_get",
        description = "Fetch a single product with full detail (price, stock, categories, type). " +
            "Use when the merchant references a specific product by ID. " +
            "For variable products use product_variations_list to inspect all variants or fetch one by variation_id.",
        inputSchema = inputSchema {
            integer("id", description = "The product ID. Required.", required = true)
            arrayEnum(
                name = "extra_fields",
                values = PRODUCTS_GET_EXTRA_FIELDS.toList(),
                description = "Optional compact fields for heavier product detail.",
            )
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        validateAllowedArguments(call.arguments, PRODUCTS_GET_ALLOWED_ARGS, descriptor.name).exceptionOrNull()?.let {
            return ToolResult.ValidationError(call.id, it.message ?: "Invalid arguments")
        }
        val extraFields = parseExtraFields(call.arguments, PRODUCTS_GET_EXTRA_FIELDS, descriptor.name).getOrElse {
            return ToolResult.ValidationError(call.id, it.message ?: "Invalid extra_fields")
        }
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        return dataSource.getProduct(args.id).fold(
            onSuccess = { product ->
                ToolResult.Success(
                    toolCallId = call.id,
                    structured = json.encodeToJsonElement(product.toProductDetailResponse(extraFields)) as JsonObject,
                )
            },
            onFailure = { ToolResult.TransportError(toolCallId = call.id, retryable = true) },
        )
    }

    @Serializable
    private data class Args(val id: Long)
}

private val PRODUCTS_GET_ALLOWED_ARGS = setOf("id", "extra_fields")
private val PRODUCTS_GET_EXTRA_FIELDS = setOf(
    "description",
    "short_description",
    "attributes",
    "images",
    "dimensions",
    "weight",
    "shipping_class",
    "cross_sell_ids",
    "upsell_ids",
    "related_ids",
)
