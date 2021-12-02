package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.CreateOrderRequest
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderCreationRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val dispatchers: CoroutineDispatchers
) {
    suspend fun createOrder(order: Order): Result<Order> {
        val status = withContext(dispatchers.io) {
            // Currently this query will run on the current thread, so forcing the usage of IO dispatcher
            orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), order.status.value)
                ?: error("Couldn't find the a status with key ${order.status.value}")
        }
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
