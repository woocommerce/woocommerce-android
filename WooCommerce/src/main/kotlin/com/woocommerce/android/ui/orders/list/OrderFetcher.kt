package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.util.WooLog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersByIdsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrdersFetchedByIds
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This Singleton is a light-weight class that provides methods for ensuring that orders matching a provided list
 * of [RemoteId]'s exist in the local DB. If they do not exist, or if they have been modified since last saved to
 * the DB, FluxC will pull down fresh versions from the API and save them to the DB. Once all orders have been
 * successfully fetched, any [androidx.paging.PagedList] views observing this specific type of data will update
 * automatically.
 */
@Singleton
class OrderFetcher @Inject constructor(private val dispatcher: Dispatcher) {
    companion object {
        private const val TAG = "OrderFetcher"
    }

    /**
     * The [RemoteId] of the [WCOrderModel] in the process of being fetched from
     * the remote API.
     */
    private val ongoingRequests = Collections.synchronizedSet(mutableSetOf<RemoteId>())

    init {
        dispatcher.register(this)
    }

    /**
     * Fetch a list of orders matching the provided list of [RemoteId]'s from the
     * API. Will first remove any [RemoteId]'s already in the process of being
     * fetched.
     *
     * @param [site] The [SiteModel] to fetch the orders from
     * @param [remoteItemIds] A list containing the [RemoteId]'s of the orders to fetch
     */
    fun fetchOrders(site: SiteModel, remoteItemIds: List<RemoteId>) {
        val idsToFetch = remoteItemIds.filter {
            // ignore duplicate requests
            !ongoingRequests.contains(it)
        }
        if (idsToFetch.isNotEmpty()) {
            ongoingRequests.addAll(idsToFetch)
            val payload = FetchOrdersByIdsPayload(site = site, remoteIds = idsToFetch)
            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersByIdsAction(payload))
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onOrdersFetchedById(event: OnOrdersFetchedByIds) {
        if (event.isError) {
            WooLog.e(WooLog.T.ORDERS, "$TAG: Error fetching orders by remoteOrderId: ${event.error.message}")
            // FIXME: Add error handling
            // FIXME: Possible add new tracks event to track error fetching order list data "order_list_load_failed"
        }
        ongoingRequests.removeAll(event.orderIds)
    }
}
