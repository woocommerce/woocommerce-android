package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import javax.inject.Inject

class GetShipments @Inject constructor(
    private val wooShippingLabelRepository: WooShippingLabelRepository,
    private val orderDetailRepository: OrderDetailRepository,
    private val productDetailRepository: ProductDetailRepository
) {
    suspend operator fun invoke(order: Order): List<ShipmentUIModel> {
        val refunds = orderDetailRepository.getOrderRefunds(order.id)
        val noRefundedProducts = refunds.getNonRefundedProducts(order.items)

        val orderItems = noRefundedProducts.mapNotNull { item ->
            val product = productDetailRepository.getProduct(item.productId)
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

        val shipments = wooShippingLabelRepository.getShipments(order.id)
        val labels = wooShippingLabelRepository.getLabels(order.id)

        var shipmentUIModelList = if (shipments.isEmpty()) {
            listOf(ShipmentUIModel(localId = "0", items = orderItems))
        } else {
            shipments.map { (shipmentId, shipmentItems) ->
                val items = shipmentItems.mapNotNull { (id, subItems) ->
                    // orderItems contains the total quantity for each product in the order.
                    // We update the quantity per shipment based on the number of subItems in each shipment.
                    orderItems.firstOrNull { it.itemId == id }
                        ?.copy(quantity = if (subItems.isNullOrEmpty()) 1f else subItems.size.toFloat())
                }
                ShipmentUIModel(localId = shipmentId, remoteId = shipmentId, items = items)
            }
        }.sortedBy { it.localId.toLong() }

        // If there are purchased labels, merge their data into the result list
        shipmentUIModelList = mergePurchaseData(shipmentUIModelList, labels)

        return shipmentUIModelList
    }

    private fun mergePurchaseData(
        shipmentUIModelList: List<ShipmentUIModel>,
        currentOrderLabels: List<ShippingLabelModel>
    ) = shipmentUIModelList.map { shipmentUIModel ->
        val shipmentLabels = currentOrderLabels.filter {
            it.shipmentId == shipmentUIModel.remoteId && it.status != ShippingLabelStatus.PURCHASE_ERROR
        }.sortedByDescending { it.createdDate?.time }

        val purchasedLabel = shipmentLabels.find {
            it.status == ShippingLabelStatus.PURCHASED && it.refund == null
        }

        shipmentUIModel.copy(label = purchasedLabel ?: shipmentLabels.firstOrNull())
    }
}
