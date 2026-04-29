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
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
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
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        return dataSource.getOrder(args.id).fold(
            onSuccess = { order ->
                ToolResult.Success(
                    toolCallId = call.id,
                    structured = json.encodeToJsonElement(order.toDetail()) as JsonObject,
                )
            },
            onFailure = { ToolResult.TransportError(toolCallId = call.id, retryable = true) },
        )
    }

    @Serializable
    private data class Args(val id: Long)

    @Serializable
    private data class OrderDetail(
        val id: Long,
        val number: String,
        val status: String,
        val total: String,
        val currency: String,
        @SerialName("date_created") val dateCreated: String,
        @SerialName("payment_method_title") val paymentMethodTitle: String,
        @SerialName("customer_name") val customerName: String,
        @SerialName("customer_email") val customerEmail: String,
    )

    private fun OrderEntity.toDetail() = OrderDetail(
        id = orderId,
        number = number,
        status = status,
        total = total,
        currency = currency,
        dateCreated = dateCreated,
        paymentMethodTitle = paymentMethodTitle,
        customerName = "$billingFirstName $billingLastName".trim(),
        customerEmail = billingEmail,
    )
}
