package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetShipments @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val configDataStore: WooShippingConfigDataStore,
) {
    suspend operator fun invoke(order: Order): List<ShipmentUIModel> {
        val refunds = orderDetailRepository.getOrderRefunds(order.id)
        val noRefundedProducts = refunds.getNonRefundedProducts(order.items)

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

        val shipments = configDataStore.observeConfig(order.id).first()?.shipments

        return if (shipments.isNullOrEmpty()) {
            listOf(ShipmentUIModel(id = null, items = orderItems))
        } else {
            shipments.map { (shipmentId, shipmentItems) ->
                val items = shipmentItems.mapNotNull { (id, subItems) ->
                    // orderItems contains the total quantity for each product in the order.
                    // We update the quantity per shipment based on the number of subItems in each shipment.
                    orderItems.firstOrNull { it.itemId == id }
                        ?.copy(quantity = if (subItems.isNullOrEmpty()) 1f else subItems.size.toFloat())
                }
                ShipmentUIModel(shipmentId, items)
            }
        }
    }
}
