package com.woocommerce.android.aiassistant.tools.orders

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

internal class OrdersUpdateToolHandler @Inject constructor(
    private val dataSource: AIOrdersDataSource,
    @AiAssistantJson private val json: Json,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = "orders_update",
        description = "Update an order's status. Status changes such as completed/cancelled/refunded fire " +
            "customer emails — the merchant confirms before this dispatches. Do NOT use this to issue a " +
            "refund — moving an order to 'refunded' only changes the status, it does not return funds.",
        inputSchema = inputSchema {
            integer("id", description = "The order ID. Required.", required = true)
            enum(
                "status",
                values = listOf("pending", "processing", "on-hold", "completed", "cancelled", "refunded", "failed"),
                description = "New order status.",
                required = true,
            )
        },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        if (args.status !in ALLOWED_STATUSES) {
            return ToolResult.ValidationError(call.id, "'${args.status}' is not an allowed status.")
        }
        return dataSource.updateOrderStatus(args.id, args.status).fold(
            onSuccess = {
                ToolResult.Success(
                    toolCallId = call.id,
                    structured = json.encodeToJsonElement(
                        UpdateResult(orderId = args.id, status = args.status)
                    ) as JsonObject,
                )
            },
            onFailure = { ToolResult.TransportError(toolCallId = call.id, retryable = true) },
        )
    }

    @Serializable
    private data class Args(val id: Long, val status: String)

    @Serializable
    private data class UpdateResult(
        @SerialName("order_id") val orderId: Long,
        val status: String,
    )

    companion object {
        private val ALLOWED_STATUSES = setOf(
            "pending", "processing", "on-hold", "completed", "cancelled", "refunded", "failed"
        )
    }
}
