package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import kotlinx.serialization.json.JsonObject
import org.wordpress.android.fluxc.model.WCProductModel
import javax.inject.Inject

internal class ProductsConfirmationPreviewProvider @Inject constructor(
    private val productsDataSource: AIProductsDataSource,
) : ConfirmationPreviewProvider {
    override val key: String = "woocommerce_products"
    override val priority: Int = 100

    override fun canPreview(context: ConfirmationPreviewContext): Boolean =
        context.descriptor.name in SUPPORTED_TOOL_NAMES

    override suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview =
        when (context.descriptor.name) {
            PRODUCTS_UPDATE -> productUpdatePreview(
                arguments = context.request.arguments,
                snapshot = currentProductSnapshot(context.request.arguments),
            )
            PRODUCTS_BULK_UPDATE -> productsBulkUpdatePreview(context.request.arguments)
            else -> error("Unsupported product confirmation preview: ${context.descriptor.name}")
        }

    private fun productUpdatePreview(
        arguments: JsonObject,
        snapshot: ProductConfirmationSnapshot?,
    ): ConfirmationPreview {
        val id = arguments.longValue("id")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_product_update_generic))
        val currentValues = snapshot?.currentValues
        val fields = productFields(arguments, currentValues)
        return ConfirmationPreview(
            message = productUpdateTitle(id, snapshot?.displayName),
            fields = fields,
        )
    }

    private suspend fun productsBulkUpdatePreview(arguments: JsonObject): ConfirmationPreview {
        val ids = arguments.longArrayValue("ids")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_products_bulk_update_generic))
        val patch = arguments.objectValue("patch")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_products_bulk_update_generic))
        val displayNameById = productsDataSource.getProducts(ids)
            .getOrNull()
            ?.items
            ?.associate { it.remoteProductId to it.confirmationDisplayName() }
            .orEmpty()
        val fields = productFields(patch, currentValues = null)
        return ConfirmationPreview(
            message = quantity(
                quantity = ids.size,
                singular = R.string.ai_assistant_confirmation_products_bulk_update_title_single,
                multiple = R.string.ai_assistant_confirmation_products_bulk_update_title_multiple,
            ),
            fields = fields,
            isBulk = true,
            bulkEntries = ids.map { id -> ConfirmationBulkEntry(id, displayNameById[id]) },
        )
    }

    private suspend fun currentProductSnapshot(arguments: JsonObject): ProductConfirmationSnapshot? =
        arguments.longValue("id")
            ?.let { productId -> productsDataSource.getProduct(productId).getOrNull() }
            ?.let { product ->
                val displayName = product.confirmationDisplayName()
                ProductConfirmationSnapshot(
                    currentValues = buildMap {
                        put("name", displayName.orEmpty())
                        put("regular_price", product.regularPrice)
                        put("sale_price", product.salePrice)
                        put("stock_quantity", product.stockQuantity.formatStockQuantity())
                        put("status", product.status)
                    },
                    displayName = displayName,
                )
            }

    private data class ProductConfirmationSnapshot(
        val currentValues: Map<String, String>,
        val displayName: String?,
    )

    private fun WCProductModel.confirmationDisplayName(): String? =
        name.trim().takeIf { it.isNotEmpty() }

    private companion object {
        const val PRODUCTS_UPDATE = "products_update"
        const val PRODUCTS_BULK_UPDATE = "products_bulk_update"
        val SUPPORTED_TOOL_NAMES = setOf(PRODUCTS_UPDATE, PRODUCTS_BULK_UPDATE)
    }
}
