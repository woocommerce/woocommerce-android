package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import javax.inject.Inject

class GetShippableItems @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository,
    private val productDetailRepository: ProductDetailRepository
) {
    suspend operator fun invoke(order: Order): List<ShippableItemModel> {
        val refunds = orderDetailRepository.getOrderRefunds(order.id)
        val noRefundedProducts = refunds.getNonRefundedProducts(order.items)

        return noRefundedProducts.mapNotNull { item ->
            productDetailRepository.getProductAsync(item.productId)?.let {
                Pair(it, item)
            }
        }.filter { product ->
            product.first.isSampleProduct.not() && product.first.isVirtual.not()
        }.map {
            val (product, item) = it
            ShippableItemModel(
                itemId = item.itemId,
                productId = product.remoteId,
                height = product.height,
                width = product.width,
                length = product.length,
                weight = product.weight,
                title = product.name,
                imageUrl = product.firstImageUrl,
                quantity = item.quantity,
                price = item.total,
                currency = order.currency
            )
        }
    }
}
