package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.tools.products.AIProductVariationsDataSource
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

internal class ProductVariationsConfirmationPreviewProvider @Inject constructor(
    private val variationsDataSource: AIProductVariationsDataSource,
) : ConfirmationPreviewProvider {
    override val key: String = "woocommerce_product_variations"
    override val priority: Int = 100

    override fun canPreview(context: ConfirmationPreviewContext): Boolean =
        context.descriptor.name == PRODUCT_VARIATIONS_UPDATE

    override suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview =
        when (context.request.toolName) {
            PRODUCT_VARIATIONS_UPDATE -> WooCommerceConfirmationPreviewFormatters.productVariationUpdatePreview(
                arguments = context.request.arguments,
                currentValues = currentVariationValues(context.request.arguments),
            )
            else -> error("Unsupported variation confirmation preview: ${context.request.toolName}")
        }

    private suspend fun currentVariationValues(arguments: JsonObject): Map<String, String>? {
        val productId = WooCommerceConfirmationPreviewFormatters.run { arguments.longValue("product_id") }
        val variationId = WooCommerceConfirmationPreviewFormatters.run { arguments.longValue("id") }
        return if (productId == null || variationId == null) {
            null
        } else {
            variationsDataSource.getVariation(productId, variationId).getOrNull()?.let { variation ->
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
    }

    private fun Double.formatStockQuantity(): String =
        if (rem(1.0) == 0.0) toLong().toString() else toString()

    private companion object {
        const val PRODUCT_VARIATIONS_UPDATE = "product_variations_update"
    }
}
