package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.model.order.UpdateOrderRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.OrderUpdateStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderCreationRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val orderUpdateStore: OrderUpdateStore,
    private val orderMapper: OrderMapper,
    private val dispatchers: CoroutineDispatchers
) {
    suspend fun placeOrder(order: Order): Result<Order> {
        val status = withContext(dispatchers.io) {
            // Currently this query will run on the current thread, so forcing the usage of IO dispatcher
            orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), order.status.value)
                ?: WCOrderStatusModel(statusKey = order.status.value)
        }

        val request = UpdateOrderRequest(
            status = status,
            lineItems = order.items.map { item ->
                LineItem(
                    id = item.itemId.takeIf { it != 0L },
                    name = item.name,
                    productId = item.productId,
                    variationId = item.variationId,
                    quantity = item.quantity
                )
            },
            shippingAddress = order.shippingAddress.toShippingAddressModel(),
            billingAddress = order.billingAddress.toBillingAddressModel(),
            customerNote = order.customerNote
        )
        val result = if (order.id == 0L) {
            orderUpdateStore.createOrder(selectedSite.get(), request)
        } else {
            orderUpdateStore.updateOrder(selectedSite.get(), order.id, request)
        }

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(orderMapper.toAppModel(result.model!!))
        }
    }

    suspend fun createOrUpdateDraft(order: Order): Result<Order> {
        // TODO we need to support the "auto-draft" status here depending on Woo's version
        val status = WCOrderStatusModel(statusKey = CoreOrderStatus.PENDING.value)
        val request = UpdateOrderRequest(
            status = status,
            lineItems = order.items.map { item ->
                LineItem(
                    id = item.itemId.takeIf { it != 0L },
                    name = item.name,
                    productId = item.productId,
                    variationId = item.variationId,
                    quantity = item.quantity,
                    subtotal = item.subtotal.takeIf { item.itemId != 0L }?.toPlainString(),
                    total = item.total.takeIf { item.itemId != 0L }?.toPlainString()
                )
            },
            shippingAddress = order.shippingAddress.takeIf { it != Address.EMPTY }?.toShippingAddressModel(),
            billingAddress = order.billingAddress.takeIf { it != Address.EMPTY }?.toBillingAddressModel(),
            customerNote = order.customerNote
        )

        val result = if (order.id == 0L) {
            orderUpdateStore.createOrder(selectedSite.get(), request)
        } else {
            orderUpdateStore.updateOrder(selectedSite.get(), order.id, request)
        }

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(orderMapper.toAppModel(result.model!!))
        }
    }
}
