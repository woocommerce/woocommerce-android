package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.ProductStockStatus
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
                ProductUIModel(
                    item = item,
                    imageUrl = product?.firstImageUrl.orEmpty(),
                    isStockManaged = product?.isStockManaged ?: false,
                    stockQuantity = product?.stockQuantity ?: 0.0,
                    stockStatus = product?.stockStatus ?: ProductStockStatus.InStock
                )
                // TODO check if we need to disable the plus button depending on stock quantity
            }
        }
    }
}
