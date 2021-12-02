package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.CreateOrderRequest
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderCreationRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore
) {
    suspend fun createOrder(order: Order): Result<Order> {
        val status = orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), order.status.value)
            ?: error("Couldn't find the a status with key ${order.status.value}")
        val request = CreateOrderRequest(
            status = status,
            lineItems = order.items.map {
                LineItem(
                    name = it.name,
                    productId = it.productId,
                    variationId = it.variationId,
                    quantity = it.quantity
                )
            },
            shippingAddress = order.shippingAddress.toShippingAddressModel(),
            billingAddress = order.billingAddress.toBillingAddressModel()
        )
        val result = orderStore.createOrder(selectedSite.get(), request)

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(result.model!!.toAppModel())
        }
    }
}
