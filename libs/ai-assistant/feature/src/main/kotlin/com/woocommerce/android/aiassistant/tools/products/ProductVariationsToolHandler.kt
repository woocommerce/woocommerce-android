package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.inputSchema
import com.woocommerce.android.aiassistant.core.chat.parseArgs
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.tools.validateAllowedArguments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.wordpress.android.fluxc.model.WCProductVariationModel
import java.math.BigDecimal
import javax.inject.Inject

internal class ProductVariationsToolHandler @Inject constructor(
    private val dataSource: AIProductVariationsDataSource,
    @AiAssistantJson private val json: Json,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = "product_variations_list",
        description = "List variations for a variable product, or fetch one variation by variation_id.",
        inputSchema = inputSchema {
            integer("product_id", description = "The parent product ID. Required.", required = true)
            integer("variation_id", description = "Variation ID; omit to list variations or provide to fetch one.")
            integer("page", description = "1-based page number; default 1. List mode only.")
            integer("per_page", description = "Max items; clamped 1-50, default 20. List mode only.")
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        validateAllowedArguments(call.arguments, PRODUCT_VARIATIONS_ALLOWED_ARGS, descriptor.name).exceptionOrNull()
            ?.let {
                return ToolResult.ValidationError(call.id, it.message ?: "Invalid arguments")
            }
        val args = call.parseArgs<Args>(json).getOrElse {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${it.message}")
        }

        val result = if (args.variationId != null) {
            dataSource.getVariation(productId = args.productId, variationId = args.variationId)
                .map { variation -> variation.toDetailJson() }
        } else {
            dataSource.fetchVariations(productId = args.productId, page = args.page, perPage = args.perPage)
                .map { variations -> variations.toListJson(productId = args.productId) }
        }

        return result.fold(
            onSuccess = { ToolResult.Success(toolCallId = call.id, structured = it) },
            onFailure = { ToolResult.TransportError(toolCallId = call.id, retryable = true) },
        )
    }

    @Serializable
    private data class Args(
        @SerialName("product_id") val productId: Long,
        @SerialName("variation_id") val variationId: Long? = null,
        val page: Int = 1,
        @SerialName("per_page") val perPage: Int = 20,
    )

    @Serializable
    private data class PriceRange(
        val min: String,
        val max: String,
    )

    @Serializable
    private data class VariationList(
        @SerialName("product_id") val productId: Long,
        val count: Int,
        val ids: List<Long>,
        val variations: List<ProductVariationDetailResponse>,
        @SerialName("stock_status_counts") val stockStatusCounts: Map<String, Int>,
        @SerialName("price_range") val priceRange: PriceRange?,
    )

    private fun WCProductVariationModel.toDetailJson(): JsonObject =
        json.encodeToJsonElement(toProductVariationDetailResponse()) as JsonObject

    private fun List<WCProductVariationModel>.toListJson(productId: Long): JsonObject {
        val response = VariationList(
            productId = productId,
            count = size,
            ids = map { it.remoteVariationId.value },
            variations = map { it.toProductVariationDetailResponse() },
            stockStatusCounts = groupingBy { it.stockStatus }.eachCount(),
            priceRange = priceRange(),
        )
        val encoded = json.encodeToJsonElement(response) as JsonObject
        return if (response.priceRange == null) {
            buildJsonObject {
                encoded.forEach { (key, value) -> put(key, value) }
                put("price_range", JsonNull)
            }
        } else {
            encoded
        }
    }

    private fun List<WCProductVariationModel>.priceRange(): PriceRange? {
        val pricedVariations = mapNotNull { variation ->
            variation.price.toBigDecimalOrNull()?.let { price -> price to variation.price }
        }
        if (pricedVariations.isEmpty()) return null

        val minPrice = pricedVariations.minBy { it.first }.second
        val maxPrice = pricedVariations.maxBy { it.first }.second
        return PriceRange(min = minPrice, max = maxPrice)
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        takeIf { it.isNotBlank() }?.let { runCatching { it.toBigDecimal() }.getOrNull() }
}

private val PRODUCT_VARIATIONS_ALLOWED_ARGS = setOf("product_id", "variation_id", "page", "per_page")
