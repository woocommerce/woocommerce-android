package com.woocommerce.android.background

import com.woocommerce.android.ui.orders.filters.domain.GetWCOrderListDescriptorWithFilters
import com.woocommerce.android.ui.orders.list.StoreOrdersListLastUpdate
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class UpdateOrderListBySelectedStore @Inject constructor(
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters,
    private val listStore: ListStore,
    private val ordersStore: WCOrderStore,
    private val storeOrdersListLastUpdate: StoreOrdersListLastUpdate
) {
    suspend operator fun invoke(deleteOldData: Boolean = false): Result<Unit> {
        val listDescriptor = getWCOrderListDescriptorWithFilters()
        val response = ordersStore.fetchOrdersListFirstPage(listDescriptor, deleteOldData)
        val firstPage = response.model

        return when {
            response.isError -> return Result.failure(
                Exception("${UpdateOrderListBySelectedStore::class.simpleName} ${response.error.message}")
            )

            firstPage == null -> Result.failure(
                Exception("${UpdateOrderListBySelectedStore::class.simpleName} no orders fetched")
            )

            else -> {
                listStore.saveListFetched(
                    listDescriptor = listDescriptor,
                    remoteItemIds = firstPage.orders.map { it.orderId },
                    canLoadMore = firstPage.canLoadMore
                )
                storeOrdersListLastUpdate(listDescriptor.uniqueIdentifier.value)
                Result.success(Unit)
            }
        }
    }
}
