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
        description = "Update a single order. Accepts only the `status` field. " +
            "Allowed transitions: on-hold -> processing, processing -> completed. " +
            "Cancellation and refund flows are not supported by this tool. " +
            "At most one write is executed per turn; additional write calls in the same turn will be " +
            "rejected by the runtime.",
        inputSchema = inputSchema {
            integer("id", description = "The order ID.", required = true)
            enum(
                "status",
                values = listOf("processing", "completed"),
                description = "New status. Allowed transitions: on-hold → processing, processing → completed.",
                required = true,
            )
        },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }

        val currentOrder = dataSource.getOrder(args.id).getOrElse {
            return ToolResult.TransportError(toolCallId = call.id, retryable = true)
        }

        val allowedTarget = ALLOWED_TRANSITIONS[currentOrder.status]
        if (allowedTarget != args.status) {
            return ToolResult.ValidationError(
                call.id,
                "Cannot transition from '${currentOrder.status}' to '${args.status}'. " +
                    "Allowed transitions: on-hold → processing, processing → completed.",
            )
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
        private val ALLOWED_TRANSITIONS = mapOf(
            "on-hold" to "processing",
            "processing" to "completed",
        )
    }
}
