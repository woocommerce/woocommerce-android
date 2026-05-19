package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.tools.products.AIProductVariationsDataSource
import kotlinx.serialization.json.JsonObject
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
            currentValues = currentVariationValues(context.request.arguments),
        )

    private fun productVariationUpdatePreview(
        arguments: JsonObject,
        currentValues: Map<String, String>?,
    ): ConfirmationPreview {
        val productId = arguments.longValue("product_id")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_product_variation_update_generic))
        val variationId = arguments.longValue("id")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_product_variation_update_generic))
        val fields = variationFields(arguments, currentValues)
        return ConfirmationPreview(
            message = string(
                R.string.ai_assistant_confirmation_product_variation_update_title,
                raw(variationId.toString()),
                raw(productId.toString()),
            ),
            fields = fields,
        )
    }

    private suspend fun currentVariationValues(arguments: JsonObject): Map<String, String>? {
        val productId = arguments.longValue("product_id") ?: return null
        val variationId = arguments.longValue("id") ?: return null
        return variationsDataSource.getVariation(productId, variationId).getOrNull()?.let { variation ->
            buildMap {
                put("regular_price", variation.regularPrice)
                put("sale_price", variation.salePrice)
                put("stock_quantity", variation.stockQuantity.formatStockQuantity())
                put("stock_status", variation.stockStatus)
                put("sku", variation.sku)
                put("status", variation.status)
            }
        }
    }

    private companion object {
        const val PRODUCT_VARIATIONS_UPDATE = "product_variations_update"
        val SUPPORTED_TOOL_NAMES = setOf(PRODUCT_VARIATIONS_UPDATE)
    }
}
