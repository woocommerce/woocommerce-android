package com.woocommerce.android.aiassistant.tools.products

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

internal class ProductsBulkUpdateToolHandler @Inject constructor(
    private val dataSource: AIProductsDataSource,
    @AiAssistantJson private val json: Json,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = TOOL_NAME,
        description = "Update multiple products with the same patch. Accepts name, regular_price, " +
            "sale_price, stock_quantity, and status. Setting stock_quantity also enables stock management. " +
            "Bulk writes require confirmation.",
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
                        putJsonObject("name") { put("type", "string") }
                        putJsonObject("regular_price") { put("type", "string") }
                        putJsonObject("sale_price") { put("type", "string") }
                        putJsonObject("stock_quantity") { put("type", "integer") }
                        putJsonObject("status") {
                            put("type", "string")
                            putJsonArray("enum") { ALLOWED_STATUSES.forEach { add(it) } }
                        }
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

    @Suppress("ReturnCount")
    override suspend fun execute(call: ToolCall): ToolResult {
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        if (args.ids.isEmpty()) {
            return ToolResult.ValidationError(call.id, "At least one product ID must be provided.")
        }
        if (args.ids.size > MAX_IDS) {
            return ToolResult.ValidationError(call.id, "Cannot update more than $MAX_IDS products at once.")
        }
        if (!args.patch.hasUpdates()) {
            return ToolResult.ValidationError(call.id, "At least one product field must be provided.")
        }
        if (args.patch.status != null && args.patch.status !in ALLOWED_STATUSES) {
            return ToolResult.ValidationError(call.id, "'${args.patch.status}' is not an allowed status.")
        }

        return dataSource.bulkUpdateProducts(
            productIds = args.ids,
            update = AIProductsDataSource.ProductUpdate(
                name = args.patch.name,
                regularPrice = args.patch.regularPrice,
                salePrice = args.patch.salePrice,
                stockQuantity = args.patch.stockQuantity,
                status = args.patch.status,
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
        val name: String? = null,
        @SerialName("regular_price") val regularPrice: String? = null,
        @SerialName("sale_price") val salePrice: String? = null,
        @SerialName("stock_quantity") val stockQuantity: Int? = null,
        val status: String? = null,
    ) {
        fun hasUpdates(): Boolean =
            name != null ||
                regularPrice != null ||
                salePrice != null ||
                stockQuantity != null ||
                status != null
    }

    private fun AIProductsDataSource.BulkUpdateResult.toJson(): JsonObject = buildJsonObject {
        put("tool", TOOL_NAME)
        put("updated_count", updatedIds.size)
        put("failed_count", failedProducts.size)
        if (updatedIds.isNotEmpty()) {
            putJsonArray("updated_ids") { updatedIds.forEach { add(it) } }
        }
        if (failedProducts.isNotEmpty()) {
            putJsonArray("failed") {
                failedProducts.forEach { failedProduct ->
                    addJsonObject {
                        put("id", failedProduct.id)
                        put("code", failedProduct.errorCode)
                        put("message", failedProduct.errorMessage)
                        put("status", failedProduct.errorStatus)
                    }
                }
            }
        }
    }

    private companion object {
        const val TOOL_NAME = "products_bulk_update"
        const val MAX_IDS = 100
        val ALLOWED_STATUSES = listOf("draft", "pending", "private", "publish")
    }
}
