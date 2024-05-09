package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.orders.OrdersListViewModel.OrderItem
import javax.inject.Inject
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OrdersForWearablesResult.Success

class OrdersRepository @Inject constructor(
    private val orderStore: WCOrderStore
) {
    suspend fun fetchOrders(
        selectedSite: SiteModel
    ): List<OrderItem> {
        return when (val result = orderStore.fetchOrdersForWearables(selectedSite)) {
            is Success -> {
                result.orders.map {
                    OrderItem(
                        date = it.dateCreated,
                        number = it.number,
                        customerName = it.billingFirstName,
                        total = it.total,
                        status = it.status
                    )
                }
            }
            else -> emptyList()
        }
    }
}
