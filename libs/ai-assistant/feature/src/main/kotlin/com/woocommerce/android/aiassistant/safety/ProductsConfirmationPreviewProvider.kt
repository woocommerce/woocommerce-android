package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

internal class ProductsConfirmationPreviewProvider @Inject constructor(
    private val productsDataSource: AIProductsDataSource,
) : ConfirmationPreviewProvider {
    override val key: String = "woocommerce_products"
    override val priority: Int = 100

    override fun canPreview(context: ConfirmationPreviewContext): Boolean =
        context.descriptor.name in SUPPORTED_TOOL_NAMES

    override suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview =
        when (context.request.toolName) {
            PRODUCTS_UPDATE -> WooCommerceConfirmationPreviewFormatters.productUpdatePreview(
                arguments = context.request.arguments,
                currentValues = currentProductValues(context.request.arguments),
            )
            PRODUCTS_BULK_UPDATE -> WooCommerceConfirmationPreviewFormatters.productsBulkUpdatePreview(
                context.request.arguments
            )
            else -> error("Unsupported product confirmation preview: ${context.request.toolName}")
        }

    private suspend fun currentProductValues(arguments: JsonObject): Map<String, String>? =
        WooCommerceConfirmationPreviewFormatters.run { arguments.longValue("id") }
            ?.let { productId -> productsDataSource.getProduct(productId).getOrNull() }
            ?.let { product ->
                buildMap {
                    put("name", product.name)
                    put("regular_price", product.regularPrice)
                    put("sale_price", product.salePrice)
                    put("stock_quantity", product.stockQuantity.formatStockQuantity())
                    put("status", product.status)
                }
            }

    private fun Double.formatStockQuantity(): String =
        if (rem(1.0) == 0.0) toLong().toString() else toString()

    private companion object {
        const val PRODUCTS_UPDATE = "products_update"
        const val PRODUCTS_BULK_UPDATE = "products_bulk_update"
        val SUPPORTED_TOOL_NAMES = setOf(PRODUCTS_UPDATE, PRODUCTS_BULK_UPDATE)
    }
}
