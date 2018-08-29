package com.woocommerce.android.ui.orders

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject

class OrderListPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus
) : OrderListContract.Presenter {
    companion object {
        private val TAG: String = OrderListPresenter::class.java.simpleName
    }

    private var orderView: OrderListContract.View? = null
    private var isLoadingOrders = false
    private var isLoadingMoreOrders = false
    private var canLoadMore = false

    override fun takeView(view: OrderListContract.View) {
        orderView = view
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)
    }

    override fun dropView() {
        orderView = null
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
    }

    override fun loadOrders(filterByStatus: String?, forceRefresh: Boolean) {
        if (networkStatus.isConnected() && forceRefresh) {
            isLoadingOrders = true
            orderView?.setLoadingIndicator(active = true)
            orderView?.showSkeleton(true)
            val payload = FetchOrdersPayload(selectedSite.get(), filterByStatus)
            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
        } else {
            fetchAndLoadOrdersFromDb(filterByStatus, isForceRefresh = false)
        }
    }

    override fun isLoading(): Boolean {
        return isLoadingOrders || isLoadingMoreOrders
    }

    override fun canLoadMore(): Boolean {
        return canLoadMore
    }

    override fun loadMoreOrders(orderStatusFilter: String?) {
        if (!networkStatus.isConnected()) return

        orderView?.setLoadingMoreIndicator(true)
        isLoadingMoreOrders = true
        val payload = FetchOrdersPayload(selectedSite.get(), orderStatusFilter, loadMore = true)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        orderView?.showSkeleton(false)
        when (event.causeOfChange) {
            FETCH_ORDERS -> {
                if (event.isError) {
                    WooLog.e(T.ORDERS, "$TAG - Error fetching orders : ${event.error.message}")
                    orderView?.showLoadOrdersError()
                    fetchAndLoadOrdersFromDb(event.statusFilter, false)
                } else {
                    canLoadMore = event.canLoadMore
                    val isForceRefresh = !isLoadingMoreOrders
                    fetchAndLoadOrdersFromDb(event.statusFilter, isForceRefresh)
                }

                if (isLoadingMoreOrders) {
                    isLoadingMoreOrders = false
                    orderView?.setLoadingMoreIndicator(active = false)
                } else {
                    isLoadingOrders = false
                    orderView?.setLoadingIndicator(active = false)
                }
            }
            // A child fragment made a change that requires a data refresh.
            UPDATE_ORDER_STATUS -> orderView?.refreshFragmentState()
            else -> {}
        }
    }

    override fun openOrderDetail(order: WCOrderModel) {
        AnalyticsTracker.trackWithSiteDetails(Stat.OPENED_ORDER_DETAIL, selectedSite.get())
        orderView?.openOrderDetail(order)
    }

    /**
     * Fetch orders from the local database.
     *
     * @param orderStatusFilter If not null, only pull orders whose status matches this filter. Default null.
     * @param isForceRefresh True if orders were refreshed from the API, else false.
     */
    override fun fetchAndLoadOrdersFromDb(orderStatusFilter: String?, isForceRefresh: Boolean) {
        val orders = orderStatusFilter?.let {
            orderStore.getOrdersForSite(selectedSite.get(), it)
        } ?: orderStore.getOrdersForSite(selectedSite.get())
        orderView?.let { view ->
            view.setLoadingIndicator(false)
            if (orders.count() > 0) {
                view.showOrders(orders, orderStatusFilter, isForceRefresh)
            } else {
                if (!networkStatus.isConnected()) {
                    // if the device if offline with no cached orders to display, show the loading
                    // indicator until a successful online refresh.
                    view.setLoadingIndicator(true)
                } else {
                    view.showNoOrders()
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh data now that a connection is active if needed
            orderView?.let { order ->
                if (order.isRefreshPending) {
                    order.refreshFragmentState()
                }
            }
        }
    }
}
