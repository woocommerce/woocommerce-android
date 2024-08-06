package com.woocommerce.android.background

import com.woocommerce.android.ui.orders.list.GetWCOrderListDescriptorWithFiltersBySiteId
import com.woocommerce.android.ui.orders.list.StoreOrdersListLastUpdate
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class UpdateOrdersListByStoreId @Inject constructor(
    private val getWCOrderListDescriptorWithFiltersBySiteId: GetWCOrderListDescriptorWithFiltersBySiteId,
    private val listStore: ListStore,
    private val ordersStore: WCOrderStore,
    private val storeOrdersListLastUpdate: StoreOrdersListLastUpdate
) {
    suspend operator fun invoke(siteId: Long, deleteOldData: Boolean = false): Boolean {
        val listDescriptor = getWCOrderListDescriptorWithFiltersBySiteId(siteId) ?: return false
        val response = ordersStore.fetchOrdersListFirstPage(listDescriptor, deleteOldData)
        val orders = response.model

        return if (response.isError || orders == null) {
            false
        } else {
            orders
                .map { it.orderId }
                .let { remoteIds ->
                    listStore.saveListFetched(
                        listDescriptor = listDescriptor,
                        remoteItemIds = remoteIds,
                        canLoadMore = remoteIds.size == listDescriptor.config.networkPageSize
                    )
                }

            storeOrdersListLastUpdate(listDescriptor.uniqueIdentifier.value)
            true
        }
    }
}
