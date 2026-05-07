package com.woocommerce.android.aiassistant.tools.orders

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolFailureKind
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
import org.wordpress.android.fluxc.store.WCOrderStore
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
            onFailure = { error ->
                ToolResult.TransportError(
                    toolCallId = call.id,
                    retryable = true,
                    kind = error.toOrderUpdateFailureKind(),
                )
            },
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
        // Allowed `status` values for `orders_update`
        // Intentionally excludes "trash" to prevent accidental deletion of orders
        private val ALLOWED_STATUSES = setOf(
            "pending",
            "processing",
            "on-hold",
            "completed",
            "cancelled",
            "refunded",
            "failed"
        )
    }
}

private fun Throwable.toOrderUpdateFailureKind(): ToolFailureKind {
    val orderError = (this as? OnChangedException)?.error as? WCOrderStore.OrderError
        ?: return ToolFailureKind.OUTCOME_UNKNOWN

    return when (orderError.type) {
        WCOrderStore.OrderErrorType.INVALID_ID,
        WCOrderStore.OrderErrorType.INVALID_PARAM,
        WCOrderStore.OrderErrorType.ORDER_STATUS_NOT_FOUND,
        WCOrderStore.OrderErrorType.EMPTY_BILLING_EMAIL -> ToolFailureKind.DETERMINISTIC_FAILURE
        WCOrderStore.OrderErrorType.GENERIC_ERROR -> if (orderError.isLocalMissingOrder()) {
            ToolFailureKind.DETERMINISTIC_FAILURE
        } else {
            ToolFailureKind.OUTCOME_UNKNOWN
        }
        else -> ToolFailureKind.OUTCOME_UNKNOWN
    }
}

private fun WCOrderStore.OrderError.isLocalMissingOrder(): Boolean =
    message == "Order not found" ||
        LOCAL_MISSING_ORDER_MESSAGE_REGEX.matches(message)

private val LOCAL_MISSING_ORDER_MESSAGE_REGEX = Regex("""^Order with id \d+ not found$""")
