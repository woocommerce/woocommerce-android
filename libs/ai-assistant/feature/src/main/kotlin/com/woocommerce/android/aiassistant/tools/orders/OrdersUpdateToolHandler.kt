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
import com.woocommerce.android.aiassistant.tools.ToolFailureDiagnosticsFactory
import com.woocommerce.android.aiassistant.tools.validateAllowedArguments
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
    private val diagnosticsFactory: ToolFailureDiagnosticsFactory,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = "orders_update",
        description = "Update an order's status, customer_note, or billing_email. Status changes such as " +
            "completed/cancelled fire customer emails — the merchant confirms before this dispatches. " +
            "Do NOT use this to issue refunds.",
        inputSchema = inputSchema {
            integer("id", description = "The order ID. Required.", required = true)
            enum(
                "status",
                values = ALLOWED_STATUSES.toList(),
                description = "New order status. Refunds are not allowed through chat tools.",
            )
            string(
                "customer_note",
                description = "Customer note to save on the order.",
                maxLength = ORDER_CUSTOMER_NOTE_MAX_LENGTH,
            )
            string(
                "billing_email",
                description = "Billing email to save on the order.",
                maxLength = ORDER_BILLING_EMAIL_MAX_LENGTH,
                format = "email",
            )
        },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )

    @Suppress("ReturnCount")
    override suspend fun execute(call: ToolCall): ToolResult {
        validateAllowedArguments(call.arguments, ORDERS_UPDATE_ALLOWED_ARGS, descriptor.name).exceptionOrNull()?.let {
            return ToolResult.ValidationError(call.id, it.message ?: "Invalid arguments")
        }
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        if (!args.hasUpdates()) {
            return ToolResult.ValidationError(call.id, "At least one order field must be provided.")
        }
        if (args.status != null && args.status !in ALLOWED_STATUSES) {
            return ToolResult.ValidationError(call.id, "'${args.status}' is not an allowed status.")
        }
        validateOrderWriteArguments(args.customerNote, args.billingEmail)?.let {
            return ToolResult.ValidationError(call.id, it)
        }
        return dataSource.updateOrder(args.id, args.toPatch()).fold(
            onSuccess = {
                dataSource.getOrder(args.id).fold(
                    onSuccess = { order ->
                        ToolResult.Success(
                            toolCallId = call.id,
                            structured = json.encodeToJsonElement(order.toOrderDetailResponse()) as JsonObject,
                        )
                    },
                    onFailure = {
                        ToolResult.Success(
                            toolCallId = call.id,
                            structured = json.encodeToJsonElement(
                                UpdateResult(
                                    orderId = args.id,
                                    status = args.status,
                                    responsePartial = true,
                                    warning = "Order updated, but updated order details could not be fetched.",
                                )
                            ) as JsonObject,
                        )
                    },
                )
            },
            onFailure = { error ->
                diagnosticsFactory.transportError(
                    toolCallId = call.id,
                    toolName = descriptor.name,
                    error = error,
                    retryable = true,
                    kind = error.toOrderUpdateFailureKind(),
                )
            },
        )
    }

    @Serializable
    private data class Args(
        val id: Long,
        val status: String? = null,
        @SerialName("customer_note") val customerNote: String? = null,
        @SerialName("billing_email") val billingEmail: String? = null,
    ) {
        fun hasUpdates(): Boolean = status != null || customerNote != null || billingEmail != null

        fun toPatch(): AIOrdersDataSource.OrderPatch = AIOrdersDataSource.OrderPatch(
            status = status,
            customerNote = customerNote,
            billingEmail = billingEmail,
        )
    }

    @Serializable
    private data class UpdateResult(
        @SerialName("order_id") val orderId: Long,
        val status: String? = null,
        @SerialName("response_partial") val responsePartial: Boolean = false,
        val warning: String? = null,
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
            "failed"
        )
    }
}

private val ORDERS_UPDATE_ALLOWED_ARGS = setOf("id", "status", "customer_note", "billing_email")

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
