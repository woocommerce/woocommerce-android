package com.woocommerce.android.ui.orders.creation.addcustomer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload
import org.wordpress.android.fluxc.store.ListStore.ListError
import org.wordpress.android.fluxc.store.ListStore.ListErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCCustomerStore
import javax.inject.Inject

class AddCustomerListItemDataSource
@Inject
constructor(
    private val store: WCCustomerStore,
    private val dispatcher: Dispatcher
) : ListItemDataSourceInterface<AddCustomerListDescriptor, Long, CustomerListItemType> {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun getItemsAndFetchIfNecessary(
        listDescriptor: WCCustomerListDescriptor,
        itemIdentifiers: List<Long>
    ): List<CustomerListItemType> {
        val storedCustomers = store.getCustomerByRemoteIds(listDescriptor.site, itemIdentifiers)
        coroutineScope.launch {
            val remoteIdToFetch = itemIdentifiers - storedCustomers.map { it.remoteCustomerId }
            if (remoteIdToFetch.isEmpty()) return@launch
            val payload = store.fetchCustomersByIdsAndCache(
                site = listDescriptor.site,
                pageSize = PAGE_SIZE,
                remoteCustomerIds = remoteIdToFetch
            )
            if (payload.error == null) {
                val listTypeIdentifier = AddCustomerListDescriptor.calculateTypeIdentifier(listDescriptor.site.id)
                dispatcher.dispatch(ListActionBuilder.newListDataInvalidatedAction(listTypeIdentifier))
            }
        }
        return itemIdentifiers.map { remoteId ->
            val customer = storedCustomers.firstOrNull { it.remoteCustomerId == remoteId }
            if (customer == null) {
                CustomerListItemType.LoadingItem(remoteId)
            } else {
                CustomerListItemType.CustomerItem(
                    remoteCustomerId = customer.remoteCustomerId,
                    email = customer.email
                )
            }
        }
    }

    override fun getItemIdentifiers(
        listDescriptor: WCCustomerListDescriptor,
        remoteItemIds: List<RemoteId>,
        isListFullyFetched: Boolean
    ): List<Long> = remoteItemIds.map { it.value }

    override fun fetchList(listDescriptor: AddCustomerListDescriptor, offset: Long) {
        coroutineScope.launch {
            val payload = store.fetchCustomers(
                offset = offset,
                pageSize = PAGE_SIZE
            )

            dispatchEventWhenFetched(listDescriptor, payload, offset)
        }
    }

    private fun dispatchEventWhenFetched(
        listDescriptor: AddCustomerListDescriptor,
        payload: WooResult<List<WCCustomerModel>>,
        offset: Long
    ) {
        dispatcher.dispatch(ListActionBuilder.newFetchedListItemsAction(FetchedListItemsPayload(
            listDescriptor = listDescriptor,
            remoteItemIds = payload.model?.map { it.remoteCustomerId } ?: emptyList(),
            loadedMore = offset > 0,
            canLoadMore = payload.model?.size == PAGE_SIZE,
            error = payload.error?.let { fetchError ->
                ListError(type = GENERIC_ERROR, message = fetchError.message)
            }
        )))
    }
}
