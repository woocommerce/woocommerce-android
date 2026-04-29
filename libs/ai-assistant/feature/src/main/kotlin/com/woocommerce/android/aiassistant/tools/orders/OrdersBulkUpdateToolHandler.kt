package com.woocommerce.android.aiassistant.tools.orders

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.parseArgs
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject

internal class OrdersBulkUpdateToolHandler @Inject constructor(
    private val dataSource: AIOrdersDataSource,
    @AiAssistantJson private val json: Json,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = TOOL_NAME,
        description = "Update multiple orders with the same patch. Accepts status, customer_note, and " +
            "billing_email. Status changes such as completed/cancelled/refunded can trigger customer emails. " +
            "Bulk writes require confirmation. Do NOT use this to issue refunds.",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonObject("properties") {
                putJsonObject("ids") {
                    put("type", "array")
                    put("minItems", 1)
                    put("maxItems", MAX_IDS)
                    putJsonObject("items") { put("type", "integer") }
                }
                putJsonObject("patch") {
                    put("type", "object")
                    put("additionalProperties", false)
                    put("minProperties", 1)
                    putJsonObject("properties") {
                        putJsonObject("status") {
                            put("type", "string")
                            putJsonArray("enum") { ALLOWED_STATUSES.forEach { add(it) } }
                        }
                        putJsonObject("customer_note") { put("type", "string") }
                        putJsonObject("billing_email") { put("type", "string") }
                    }
                }
            }
            putJsonArray("required") {
                add("ids")
                add("patch")
            }
        },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        if (args.ids.isEmpty()) {
            return ToolResult.ValidationError(call.id, "At least one order ID must be provided.")
        }
        if (args.ids.size > MAX_IDS) {
            return ToolResult.ValidationError(call.id, "Cannot update more than $MAX_IDS orders at once.")
        }
        if (!args.patch.hasUpdates()) {
            return ToolResult.ValidationError(call.id, "At least one order field must be provided.")
        }
        if (args.patch.status != null && args.patch.status !in ALLOWED_STATUSES) {
            return ToolResult.ValidationError(call.id, "'${args.patch.status}' is not an allowed status.")
        }

        return dataSource.bulkUpdateOrders(
            orderIds = args.ids,
            patch = AIOrdersDataSource.OrderPatch(
                status = args.patch.status,
                customerNote = args.patch.customerNote,
                billingEmail = args.patch.billingEmail,
            )
        ).fold(
            onSuccess = { result ->
                ToolResult.Success(
                    toolCallId = call.id,
                    structured = result.toJson(),
                )
            },
            onFailure = { ToolResult.TransportError(toolCallId = call.id, retryable = true) },
        )
    }

    @Serializable
    private data class Args(
        val ids: List<Long>,
        val patch: Patch,
    )

    @Serializable
    private data class Patch(
        val status: String? = null,
        @SerialName("customer_note") val customerNote: String? = null,
        @SerialName("billing_email") val billingEmail: String? = null,
    ) {
        fun hasUpdates(): Boolean = status != null || customerNote != null || billingEmail != null
    }

    private fun AIOrdersDataSource.BulkUpdateResult.toJson(): JsonObject = buildJsonObject {
        put("tool", TOOL_NAME)
        put("updated_count", updatedIds.size)
        put("failed_count", failedOrders.size)
        if (updatedIds.isNotEmpty()) {
            putJsonArray("updated_ids") { updatedIds.forEach { add(it) } }
        }
        if (failedOrders.isNotEmpty()) {
            putJsonArray("failed") {
                failedOrders.forEach { failedOrder ->
                    addJsonObject {
                        put("id", failedOrder.id)
                        put("code", failedOrder.errorCode)
                        put("message", failedOrder.errorMessage)
                        put("status", failedOrder.errorStatus)
                    }
                }
            }
        }
    }

    private companion object {
        const val TOOL_NAME = "orders_bulk_update"
        const val MAX_IDS = 100
        val ALLOWED_STATUSES = listOf(
            "pending",
            "processing",
            "on-hold",
            "completed",
            "cancelled",
            "refunded",
            "failed",
        )
    }
}
