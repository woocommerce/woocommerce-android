package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import com.woocommerce.android.aiassistant.tools.products.AIProductVariationsDataSource
import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject

internal data class ConfirmationSnapshot(
    val currentValues: Map<String, String>,
)

internal open class WooCommerceConfirmationSnapshotResolver @Inject constructor(
    private val ordersDataSource: AIOrdersDataSource,
    private val productsDataSource: AIProductsDataSource,
    private val variationsDataSource: AIProductVariationsDataSource,
) {
    open suspend fun resolve(request: ConfirmationRequest): ConfirmationSnapshot? = when (request.toolName) {
        ORDERS_UPDATE -> request.arguments.longValue("id")
            ?.let { orderId -> ordersDataSource.getOrder(orderId).getOrNull() }
            ?.let { order ->
                ConfirmationSnapshot(
                    currentValues = mapOf(
                        "status" to order.status,
                    )
                )
            }
        PRODUCTS_UPDATE -> request.arguments.longValue("id")
            ?.let { productId -> productsDataSource.getProduct(productId).getOrNull() }
            ?.let { product ->
                ConfirmationSnapshot(
                    currentValues = buildMap {
                        put("name", product.name)
                        put("regular_price", product.regularPrice)
                        put("sale_price", product.salePrice)
                        put("stock_quantity", product.stockQuantity.toInt().toString())
                        put("status", product.status)
                    }
                )
            }
        PRODUCT_VARIATIONS_UPDATE -> {
            val productId = request.arguments.longValue("product_id")
            val variationId = request.arguments.longValue("id")
            if (productId == null || variationId == null) {
                null
            } else {
                variationsDataSource.getVariation(productId, variationId).getOrNull()?.let { variation ->
                    ConfirmationSnapshot(
                        currentValues = buildMap {
                            put("regular_price", variation.regularPrice)
                            put("sale_price", variation.salePrice)
                            put("stock_quantity", variation.stockQuantity.toInt().toString())
                            put("stock_status", variation.stockStatus)
                            put("sku", variation.sku)
                            put("status", variation.status)
                        }
                    )
                }
            }
        }
        else -> null
    }

    private fun JsonObject.longValue(name: String): Long? =
        this[name]?.asJsonPrimitiveOrNull()?.longOrNull

    private fun JsonElement.asJsonPrimitiveOrNull(): JsonPrimitive? = runCatching { jsonPrimitive }.getOrNull()

    private companion object {
        const val ORDERS_UPDATE = "orders_update"
        const val PRODUCTS_UPDATE = "products_update"
        const val PRODUCT_VARIATIONS_UPDATE = "product_variations_update"
    }
}
