package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShippingLabelDTO
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.util.Date
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

        val config = configDataStore.observeConfig(order.id).first()

        val shipments = config?.shipments

        val shipmentUIModelList = if (shipments.isNullOrEmpty()) {
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
        return config?.shippingLabelData?.currentOrderLabels?.let { data ->
            mergePurchaseData(shipmentUIModelList, data)
        } ?: shipmentUIModelList
    }

    private fun mergePurchaseData(
        shipmentUIModelList: List<ShipmentUIModel>,
        currentOrderLabels: List<ShippingLabelDTO>
    ) = shipmentUIModelList.map { shipmentUIModel ->
        val labelForShipment = currentOrderLabels.find { it.shipmentId.toString() == shipmentUIModel.remoteId }
        if (labelForShipment == null) {
            shipmentUIModel
        } else {
            shipmentUIModel.copy(
                purchased = true,
                labelId = labelForShipment.labelId,
                carrierId = labelForShipment.carrierId,
                trackingNumber = labelForShipment.tracking,
                status = labelForShipment.status,
                refundableAmount = labelForShipment.refundableAmount ?: BigDecimal.ZERO,
                purchaseDate = labelForShipment.createdDate?.let { Date(it) },
            )
        }
    }
}
