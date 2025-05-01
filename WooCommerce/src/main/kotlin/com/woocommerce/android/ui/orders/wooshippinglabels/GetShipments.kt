package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetShipments @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val configDataStore: WooShippingConfigDataStore,
) {
    suspend operator fun invoke(order: Order): Map<String, List<ShippableItemModel>> {
        val refunds = orderDetailRepository.getOrderRefunds(order.id)
        val noRefundedProducts = refunds.getNonRefundedProducts(order.items)

        val shipments = configDataStore.observeConfig(order.id).first()?.shipments ?: emptyMap()

        val orderItems = noRefundedProducts.mapNotNull { item ->
            val product = productDetailRepository.getProductAsync(item.productId)
            if (product != null && !product.isSampleProduct && !product.isVirtual) {
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
                    price = item.price,
                    currency = order.currency
                )
            } else {
                null
            }
        }

        // Return a map by matching each shipment key to its corresponding items in orderItems and adjusting quantity by
        // subItems count
        return shipments.mapValues { (_, shipmentItems) ->
            shipmentItems.mapNotNull { (id, subItems) ->
                orderItems.firstOrNull { it.itemId == id }?.let { item ->
                    if (subItems.isNullOrEmpty()) item else item.copy(quantity = subItems.size.toFloat())
                }
            }
        }
    }
}
