package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.bundle.OrderItemRules
import com.woocommerce.android.ui.products.GetBundledProducts
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MapItemToProductUiModel @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val variationDetailRepository: VariationDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val getBundledProducts: GetBundledProducts
) {
    suspend operator fun invoke(item: Order.Item): ProductUIModel {
        return withContext(dispatchers.io) {
            if (item.isVariation) {
                val variation = variationDetailRepository.getVariation(item.productId, item.variationId)
                ProductUIModel(
                    item = item,
                    imageUrl = variation?.image?.source.orEmpty(),
                    isStockManaged = variation?.isStockManaged ?: false,
                    stockQuantity = variation?.stockQuantity ?: 0.0,
                    stockStatus = variation?.stockStatus ?: ProductStockStatus.InStock
                )
            } else {
                val product = productDetailRepository.getProduct(item.productId)
                val isBundle = product?.productType == ProductType.BUNDLE
                val rules = if (isBundle) {
                    val builder = OrderItemRules.Builder()
                    getBundledProducts(item.productId).first().forEach { bundledProduct ->
                        builder.setChildItemRule(
                            itemId = bundledProduct.id,
                            productId = bundledProduct.bundledProductId,
                            quantityMin = bundledProduct.rules.quantityMin,
                            quantityMax = bundledProduct.rules.quantityMax,
                            quantityDefault = bundledProduct.rules.quantityDefault,
                            optional = bundledProduct.rules.isOptional
                        )
                    }
                    builder.build()
                } else {
                    null
                }
                ProductUIModel(
                    item = item,
                    imageUrl = product?.firstImageUrl.orEmpty(),
                    isStockManaged = product?.isStockManaged ?: false,
                    stockQuantity = product?.stockQuantity ?: 0.0,
                    stockStatus = product?.specialStockStatus ?: product?.stockStatus ?: ProductStockStatus.InStock,
                    rules = rules
                )
                // TODO check if we need to disable the plus button depending on stock quantity
            }
        }
    }
}
