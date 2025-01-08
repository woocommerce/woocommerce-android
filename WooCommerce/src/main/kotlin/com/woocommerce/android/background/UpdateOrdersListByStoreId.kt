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
    suspend operator fun invoke(siteId: Long, deleteOldData: Boolean = false): Result<Unit> {
        val listDescriptor = getWCOrderListDescriptorWithFiltersBySiteId(siteId)
            ?: return Result.failure(
                Exception("${UpdateOrdersListByStoreId::class.java.name} There is no list descriptor")
            )
        val response = ordersStore.fetchOrdersListFirstPage(listDescriptor, deleteOldData)
        val orders = response.model

        return when {
            response.isError -> Result.failure(
                Exception("${UpdateOrdersListByStoreId::class.java.name} ${response.error.message}")
            )

            orders == null -> Result.failure(
                Exception("${UpdateOrdersListByStoreId::class.java.name} There is no orders")
            )

            else -> {
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
                Result.success(Unit)
            }
        }
    }
}
