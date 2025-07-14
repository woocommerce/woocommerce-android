package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.util.AppLog
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FetchOrdersRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val coroutineScope: CoroutineScope,
    private val storeOrdersListLastUpdate: StoreOrdersListLastUpdate
) {
    /**
     * The [RemoteId] of the OrderEntity in the process of being fetched from
     * the remote API.
     */
    private val ongoingRequests = Collections.synchronizedSet(mutableSetOf<Long>())

    init {
        dispatcher.register(this)
    }

    /**
     * Fetch a list of orders matching the provided list of [RemoteId]'s from the
     * API. Will first remove any [RemoteId]'s already in the process of being
     * fetched.
     *
     * @param [site] The [SiteModel] to fetch the orders from
     * @param [orderIds] A list containing the ids of the orders to fetch
     */
    fun fetchOrders(site: SiteModel, orderIds: List<Long>) {
        val idsToFetch = orderIds.filter {
            // ignore duplicate requests
            !ongoingRequests.contains(it)
        }
        if (idsToFetch.isNotEmpty()) {
            ongoingRequests.addAll(idsToFetch)
            val payload = WCOrderStore.FetchOrdersByIdsPayload(site = site, orderIds = idsToFetch)
            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersByIdsAction(payload))
        }
    }

    @Suppress("unused", "ForbiddenComment")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onOrdersFetchedById(event: WCOrderStore.OnOrdersFetchedByIds) {
        if (event.isError) {
            AppLog.e(AppLog.T.API, "Error fetching orders by remoteOrderId: ${event.error.message}")
            AnalyticsTracker.track(
                AnalyticsEvent.ORDER_LIST_LOAD_ERROR,
                this.javaClass.simpleName,
                event.error.type.toString(),
                event.error.message
            )
        }
        ongoingRequests.removeAll(event.orderIds.toSet())
    }

    @Suppress("unused", "ForbiddenComment")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onOrderListFetched(event: OnListChanged) {
        if (event.isError.not() && event.causeOfChange == OnListChanged.CauseOfListChange.FIRST_PAGE_FETCHED) {
            coroutineScope.launch {
                event.listDescriptors.forEach { listDescriptor ->
                    storeOrdersListLastUpdate(listDescriptor.uniqueIdentifier.value)
                }
            }
        }
    }
}
