package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MapItemToProductUiModel @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val variationDetailRepository: VariationDetailRepository,
    private val productDetailRepository: ProductDetailRepository
) {
    suspend operator fun invoke(item: Order.Item): ProductUIModel {
        return with(item) {
            val (imageUrl, isStockManaged, stockQuantity) = withContext(dispatchers.io) {
                if (isVariation) {
                    val variation = variationDetailRepository.getVariation(productId, variationId)
                    Triple(variation?.image?.source, variation?.isStockManaged, variation?.stockQuantity)
                } else {
                    val product = productDetailRepository.getProduct(productId)
                    Triple(product?.firstImageUrl, product?.isStockManaged, product?.stockQuantity)
                }
            }
            ProductUIModel(
                item = this,
                imageUrl = imageUrl.orEmpty(),
                isStockManaged = isStockManaged ?: false,
                stockQuantity = stockQuantity ?: 0.0,
                canDecreaseQuantity = quantity >= 2
                // TODO check if we need to disable the plus button depending on stock quantity
            )
        }
    }
}
