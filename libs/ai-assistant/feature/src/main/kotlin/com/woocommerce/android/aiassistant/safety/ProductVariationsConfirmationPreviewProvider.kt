package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.tools.products.AIProductVariationsDataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.wordpress.android.fluxc.model.WCProductVariationModel
import javax.inject.Inject

internal class ProductVariationsConfirmationPreviewProvider @Inject constructor(
    private val variationsDataSource: AIProductVariationsDataSource,
) : ConfirmationPreviewProvider {
    override val key: String = "woocommerce_product_variations"
    override val priority: Int = 100

    override fun canPreview(context: ConfirmationPreviewContext): Boolean =
        context.descriptor.name in SUPPORTED_TOOL_NAMES

    override suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview =
        productVariationUpdatePreview(
            arguments = context.request.arguments,
            snapshot = currentVariationSnapshot(context.request.arguments),
        )

    private fun productVariationUpdatePreview(
        arguments: JsonObject,
        snapshot: VariationConfirmationSnapshot?,
    ): ConfirmationPreview {
        val productId = arguments.longValue("product_id")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_product_variation_update_generic))
        val variationId = arguments.longValue("id")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_product_variation_update_generic))
        val currentValues = snapshot?.currentValues
        val fields = variationFields(arguments, currentValues)
        return ConfirmationPreview(
            message = productVariationUpdateTitle(productId, variationId, snapshot?.displayName),
            fields = fields,
        )
    }

    private suspend fun currentVariationSnapshot(arguments: JsonObject): VariationConfirmationSnapshot? {
        val productId = arguments.longValue("product_id") ?: return null
        val variationId = arguments.longValue("id") ?: return null
        return variationsDataSource.getVariation(productId, variationId).getOrNull()?.let { variation ->
            VariationConfirmationSnapshot(
                currentValues = buildMap {
                    put("regular_price", variation.regularPrice)
                    put("sale_price", variation.salePrice)
                    put("stock_quantity", variation.stockQuantity.formatStockQuantity())
                    put("stock_status", variation.stockStatus)
                    put("sku", variation.sku)
                    put("status", variation.status)
                },
                displayName = variation.confirmationDisplayName(),
            )
        }
    }

    private fun productVariationUpdateTitle(
        productId: Long,
        variationId: Long,
        displayName: String?,
    ): ConfirmationPreviewText =
        displayName?.let { name ->
            string(
                R.string.ai_assistant_confirmation_product_variation_update_title_with_name,
                raw(name),
                raw(variationId.toString()),
                raw(productId.toString()),
            )
        } ?: string(
            R.string.ai_assistant_confirmation_product_variation_update_title,
            raw(variationId.toString()),
            raw(productId.toString()),
        )

    private data class VariationConfirmationSnapshot(
        val currentValues: Map<String, String>,
        val displayName: String?,
    )

    private fun WCProductVariationModel.confirmationDisplayName(): String? =
        variationAttributeOptions().takeIf { it.isNotEmpty() }
            ?: sku.trim().takeIf { it.isNotEmpty() }

    private fun WCProductVariationModel.variationAttributeOptions(): String =
        runCatching {
            displayNameJson.decodeFromString<List<VariationAttribute>>(attributes)
                .mapNotNull { it.option?.trim()?.takeIf(String::isNotEmpty) }
                .joinToString(", ")
        }.getOrDefault("")

    @Serializable
    private data class VariationAttribute(
        val option: String? = null,
    )

    private companion object {
        const val PRODUCT_VARIATIONS_UPDATE = "product_variations_update"
        val displayNameJson = Json { ignoreUnknownKeys = true }
        val SUPPORTED_TOOL_NAMES = setOf(PRODUCT_VARIATIONS_UPDATE)
    }
}
