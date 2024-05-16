package com.woocommerce.android.wear

import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.commons.wear.orders.WearOrderProduct
import javax.inject.Inject

class GetWearableOrderProducts @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository
) {
    suspend operator fun invoke(
        orderId: Long
    ): List<WearOrderProduct> {
        val orderItems = orderDetailRepository
            .fetchOrderById(orderId)?.items
            ?: return emptyList()

        return orderDetailRepository.fetchOrderRefunds(orderId)
            .getNonRefundedProducts(orderItems)
            .map {
                WearOrderProduct(
                    amount = it.quantity.toString(),
                    total = it.total.toString(),
                    name = it.name
                )
            }
    }
}
