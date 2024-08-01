package com.woocommerce.android.background

import com.woocommerce.android.ui.orders.filters.domain.GetWCOrderListDescriptorWithFilters
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class UpdateOrdersList @Inject constructor(
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters,
    private val listStore: ListStore,
    private val ordersStore: WCOrderStore
) {
    suspend operator fun invoke(): Boolean {
        val listDescriptor = getWCOrderListDescriptorWithFilters()
        val response = ordersStore.fetchOrdersListFirstPage(listDescriptor)
        val orders = response.model

        if (response.isError || orders == null) return false

        orders.map { it.orderId }.let { remoteIds ->
            listStore.saveListFetched(
                listDescriptor = listDescriptor,
                remoteItemIds = remoteIds,
                canLoadMore = remoteIds.size == listDescriptor.config.networkPageSize
            )
        }

        return true
    }
}
