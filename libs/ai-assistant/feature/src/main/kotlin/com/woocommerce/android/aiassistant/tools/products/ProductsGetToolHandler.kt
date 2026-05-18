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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject

internal class ProductsGetToolHandler @Inject constructor(
    private val dataSource: AIProductsDataSource,
    @AiAssistantJson private val json: Json,
    private val diagnosticsFactory: ToolFailureDiagnosticsFactory,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = "products_get",
        description = "Fetch a single product with full detail (price, stock, categories, type). " +
            "Use when the merchant references a specific product by ID. " +
            "For variable products, use product_variations_list only when the merchant explicitly asks about " +
            "variations, sizes, colors, options, or variation-level stock. Do NOT call this tool to render a " +
            "card after products_list - `show_cards` re-fetches product detail itself when given a reference.",
        inputSchema = inputSchema {
            integer("id", description = "The product ID. Required.", required = true)
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        validateAllowedArguments(call.arguments, PRODUCTS_GET_ALLOWED_ARGS, descriptor.name).exceptionOrNull()?.let {
            return ToolResult.ValidationError(call.id, it.message ?: "Invalid arguments")
        }
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        return dataSource.getProduct(args.id).fold(
            onSuccess = { product ->
                ToolResult.Success(
                    toolCallId = call.id,
                    structured = json.encodeToJsonElement(product.toProductDetailResponse()) as JsonObject,
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
    private data class Args(val id: Long)
}

private val PRODUCTS_GET_ALLOWED_ARGS = setOf("id")
