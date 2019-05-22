package com.woocommerce.android.ui.orders

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ActivityScope
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
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrdersSearched
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersPayload
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

@OpenClassOnDebug
@ActivityScope
class OrderListPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus
) : OrderListContract.Presenter {
    companion object {
        private val TAG: String = OrderListPresenter::class.java.simpleName
    }

    private enum class OrderListState {
        IDLE,
        LOADING,
        LOADING_MORE,
        SEARCHING,
        SEARCHING_MORE
    }

    private var orderView: OrderListContract.View? = null
    private var orderListState = OrderListState.IDLE

    private var canLoadMore = false
    private var canSearchMore = false
    private var nextSearchOffset = 0

    private var isRefreshingOrderStatusOptions = false

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
            orderListState = OrderListState.LOADING
            orderView?.showEmptyView(false)
            orderView?.showSkeleton(true)
            val payload = FetchOrdersPayload(selectedSite.get(), filterByStatus)
            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
        } else {
            fetchAndLoadOrdersFromDb(filterByStatus, isForceRefresh = false)
        }
    }

    override fun loadMoreOrders(orderStatusFilter: String?) {
        if (!networkStatus.isConnected()) return

        orderView?.setLoadingMoreIndicator(true)
        orderListState = OrderListState.LOADING_MORE
        val payload = FetchOrdersPayload(selectedSite.get(), orderStatusFilter, loadMore = true)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
    }

    override fun searchOrders(searchQuery: String) {
        nextSearchOffset = 0

        when {
            searchQuery.isBlank() -> orderView?.showSearchResults(searchQuery, emptyList())
            networkStatus.isConnected() -> {
                orderListState = OrderListState.SEARCHING
                orderView?.showSkeleton(true)
                val payload = SearchOrdersPayload(selectedSite.get(), searchQuery, 0)
                dispatcher.dispatch(WCOrderActionBuilder.newSearchOrdersAction(payload))
            }
            else -> orderView?.showNoConnectionError()
        }
    }

    override fun searchMoreOrders(searchQuery: String) {
        if (!networkStatus.isConnected()) return

        orderListState = OrderListState.SEARCHING_MORE
        orderView?.setLoadingMoreIndicator(true)
        val payload = SearchOrdersPayload(selectedSite.get(), searchQuery, nextSearchOffset)
        dispatcher.dispatch(WCOrderActionBuilder.newSearchOrdersAction(payload))
    }

    override fun isLoadingOrders(): Boolean {
        return orderListState != OrderListState.IDLE
    }

    override fun isOrderStatusOptionsRefreshing(): Boolean {
        return isRefreshingOrderStatusOptions
    }

    override fun canLoadMoreOrders(): Boolean {
        orderView?.let {
            if (it.isSearching) return canSearchMore
        }
        return canLoadMore
    }

    override fun getOrderStatusOptions(): Map<String, WCOrderStatusModel> {
        val options = orderStore.getOrderStatusOptionsForSite(selectedSite.get())
        return if (options.isEmpty()) {
            refreshOrderStatusOptions()
            emptyMap()
        } else {
            options.map { it.statusKey to it }.toMap()
        }
    }

    override fun refreshOrderStatusOptions() {
        // Refresh the order status options from the API
        if (!isOrderStatusOptionsRefreshing()) {
            isRefreshingOrderStatusOptions = true
            dispatcher.dispatch(
                    WCOrderActionBuilder
                            .newFetchOrderStatusOptionsAction(FetchOrderStatusOptionsPayload(selectedSite.get()))
            )
        }
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
                    AnalyticsTracker.track(Stat.ORDERS_LIST_LOADED, mapOf(
                            AnalyticsTracker.KEY_STATUS to event.statusFilter.orEmpty(),
                            AnalyticsTracker.KEY_IS_LOADING_MORE to (orderListState == OrderListState.LOADING_MORE)))

                    canLoadMore = event.canLoadMore
                    val isForceRefresh = orderListState != OrderListState.LOADING_MORE
                    fetchAndLoadOrdersFromDb(event.statusFilter, isForceRefresh)
                }

                if (orderListState == OrderListState.LOADING_MORE) {
                    orderView?.setLoadingMoreIndicator(active = false)
                }

                orderListState = OrderListState.IDLE
            }
            // A child fragment made a change that requires a data refresh.
            UPDATE_ORDER_STATUS -> orderView?.refreshFragmentState()
            else -> {}
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrdersSearched(event: OnOrdersSearched) {
        canSearchMore = event.canLoadMore
        nextSearchOffset = event.nextOffset

        if (event.isError) {
            WooLog.e(T.ORDERS, "$TAG - Error searching orders : ${event.error.message}")
            orderView?.showLoadOrdersError()
        } else {
            orderView?.showEmptyView(event.searchResults.isEmpty())
            if (event.searchResults.isNotEmpty()) {
                if (orderListState == OrderListState.SEARCHING_MORE) {
                    orderView?.addSearchResults(event.searchQuery, event.searchResults)
                } else {
                    orderView?.showSearchResults(event.searchQuery, event.searchResults)
                }
            }
        }

        if (orderListState == OrderListState.SEARCHING_MORE) {
            orderView?.setLoadingMoreIndicator(active = false)
        } else {
            orderView?.showSkeleton(false)
        }

        orderListState = OrderListState.IDLE
    }

    @Suppress
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderStatusOptionsChanged(event: OnOrderStatusOptionsChanged) {
        isRefreshingOrderStatusOptions = false

        if (event.isError) {
            WooLog.e(T.ORDERS, "$TAG - Error fetching order status options from the api : ${event.error.message}")
            return
        }

        if (event.rowsAffected > 0) {
            orderView?.setOrderStatusOptions(getOrderStatusOptions())
        }
    }

    override fun openOrderDetail(order: WCOrderModel) {
        AnalyticsTracker.track(Stat.ORDER_OPEN, mapOf(
                AnalyticsTracker.KEY_ID to order.remoteOrderId,
                AnalyticsTracker.KEY_STATUS to order.status))
        orderView?.let {
            if (!it.isRefreshing) {
                it.openOrderDetail(order)
            }
        }
    }

    /**
     * Fetch orders from the local database.
     *
     * @param orderStatusFilter If not null, only pull orders whose status matches this filter. Default null.
     * @param isForceRefresh True if orders were refreshed from the API, else false.
     */
    override fun fetchOrdersFromDb(orderStatusFilter: String?, isForceRefresh: Boolean): List<WCOrderModel> {
        return orderStatusFilter?.let {
            orderStore.getOrdersForSite(selectedSite.get(), it)
        } ?: orderStore.getOrdersForSite(selectedSite.get())
    }

    /**
     * Load orders from the local database and display it in UI.
     *
     * @param orderStatusFilter If not null, only pull orders whose status matches this filter. Default null.
     * @param isForceRefresh True if orders were refreshed from the API, else false.
     */
    override fun fetchAndLoadOrdersFromDb(orderStatusFilter: String?, isForceRefresh: Boolean) {
        val orders = fetchOrdersFromDb(orderStatusFilter, isForceRefresh)
        orderView?.let { view ->
            val currentOrders = removeFutureOrders(orders)
            if (currentOrders.count() > 0) {
                view.showEmptyView(false)
                view.showOrders(currentOrders, orderStatusFilter, isForceRefresh)
            } else {
                if (!networkStatus.isConnected()) {
                    // if the device is offline with no cached orders to display, show the loading
                    // indicator until a successful online refresh.
                    view.showSkeleton(true)
                } else {
                    view.showEmptyView(true)
                }
            }
        }
    }

    /**
     * Removes orders with a future creation date so we don't show them in the order list
     * https://github.com/woocommerce/woocommerce-android/issues/425
     */
    private fun removeFutureOrders(orders: List<WCOrderModel>): List<WCOrderModel> {
        val now = DateTimeUtils.nowUTC()
        val currentOrders = ArrayList<WCOrderModel>()
        orders.forEach {
            // make sure the creation date is before today, or is today
            val orderDate = DateTimeUtils.dateUTCFromIso8601(it.dateCreated) ?: Date()
            if (orderDate.time <= now.time || DateTimeUtils.daysBetween(orderDate, now) == 0) {
                currentOrders.add(it)
            } else {
                WooLog.i(T.ORDERS, "Hiding future order ${it.dateCreated}")
            }
        }
        return currentOrders
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
