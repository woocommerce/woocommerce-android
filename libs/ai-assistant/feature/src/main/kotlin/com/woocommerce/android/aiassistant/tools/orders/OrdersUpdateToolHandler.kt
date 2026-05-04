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
            "Cancellation and refund flows are not supported by this tool; never use it to issue refunds. " +
            "Writes require Android confirmation UI; do not ask for confirmation in prose. " +
            "Use at most one write per assistant turn; this is one single-entity write outside explicit bulk tools.",
        inputSchema = inputSchema {
            integer("id", description = "The order ID. Required.", required = true)
            enum(
                "status",
                values = ALLOWED_STATUSES.toList(),
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
        return execute(call, args)
    }

    private suspend fun execute(call: ToolCall, args: Args): ToolResult {
        if (args.status !in ALLOWED_STATUSES) {
            return ToolResult.ValidationError(call.id, "'${args.status}' is not an allowed status.")
        }
        val currentStatus = dataSource.getOrder(args.id).getOrElse {
            return ToolResult.TransportError(call.id, retryable = true)
        }.status.normalizedOrderStatus()
        if (args.status !in ALLOWED_TRANSITIONS[currentStatus].orEmpty()) {
            return ToolResult.ValidationError(
                call.id,
                "Cannot transition order from '$currentStatus' to '${args.status}'.",
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

    private fun String.normalizedOrderStatus(): String = removePrefix("wc-")

    private companion object {
        private val ALLOWED_STATUSES = setOf(
            "processing",
            "completed",
        )
        private val ALLOWED_TRANSITIONS = mapOf(
            "on-hold" to setOf("processing"),
            "processing" to setOf("completed"),
        )
    }
}
