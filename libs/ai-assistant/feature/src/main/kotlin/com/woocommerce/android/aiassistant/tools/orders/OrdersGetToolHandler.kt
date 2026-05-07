package com.woocommerce.android.aiassistant.tools.orders

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

internal class OrdersGetToolHandler @Inject constructor(
    private val dataSource: AIOrdersDataSource,
    @AiAssistantJson private val json: Json,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = "orders_get",
        description = "Fetch a single order with full detail (line items, billing/shipping, status, customer_id). " +
            "Use when the merchant references a specific order by ID. The customer_id can chain into " +
            "customers_list for follow-up questions about the buyer.",
        inputSchema = inputSchema {
            integer("id", description = "The order ID.", required = true)
            arrayEnum(
                name = "extra_fields",
                values = ORDERS_GET_EXTRA_FIELDS.toList(),
                description = "Optional compact fields: billing, shipping, coupon_lines, fee_lines, tax_lines.",
            )
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        validateAllowedArguments(call.arguments, ORDERS_GET_ALLOWED_ARGS, descriptor.name).exceptionOrNull()?.let {
            return ToolResult.ValidationError(call.id, it.message ?: "Invalid arguments")
        }
        val extraFields = parseExtraFields(call.arguments, ORDERS_GET_EXTRA_FIELDS, descriptor.name).getOrElse {
            return ToolResult.ValidationError(call.id, it.message ?: "Invalid extra_fields")
        }
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        return dataSource.getOrder(args.id).fold(
            onSuccess = { order ->
                ToolResult.Success(
                    toolCallId = call.id,
                    structured = json.encodeToJsonElement(order.toOrderDetailResponse(extraFields)) as JsonObject,
                )
            },
            onFailure = {
                // TODO Improve retryable detection logic to avoid unnecessary retries.
                ToolResult.TransportError(toolCallId = call.id, retryable = true)
            },
        )
    }

    @Serializable
    private data class Args(val id: Long)
}

private val ORDERS_GET_ALLOWED_ARGS = setOf("id", "extra_fields")
private val ORDERS_GET_EXTRA_FIELDS = setOf("billing", "shipping", "coupon_lines", "fee_lines", "tax_lines")
