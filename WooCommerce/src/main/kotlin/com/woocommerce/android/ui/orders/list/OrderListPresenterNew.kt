package com.woocommerce.android.ui.orders.list

import androidx.lifecycle.Lifecycle
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import javax.inject.Inject

@ActivityScope
class OrderListPresenterNew @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val listStore: ListStore
) : OrderListContractNew.Presenter {
    companion object {
        const val TAG: String = "OrderListPresenterNew"
    }

    private var orderView: OrderListContractNew.View? = null
    private var isRefreshingOrderStatusOptions = false
    override var isShipmentTrackingProviderFetched: Boolean = false

    override fun takeView(view: OrderListContractNew.View) {
        orderView = view
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)
    }

    override fun dropView() {
        orderView = null
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
    }

    override fun generatePageWrapper(
        descriptor: WCOrderListDescriptor,
        lifecycle: Lifecycle
    ): PagedListWrapper<OrderListItemUIType> {
        return listStore.getList(
                listDescriptor = descriptor,
                dataSource = OrderListItemDataSource(dispatcher, orderStore, lifecycle),
                lifecycle = lifecycle)
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

    override fun isOrderStatusOptionsRefreshing() = isRefreshingOrderStatusOptions

    /**
     * Pre-load shipment tracking providers only if it is not already fetched
     * If it is not fetched, and if network is connected, fetch list from api
     */
    override fun loadShipmentTrackingProviders() {
        if (!isShipmentTrackingProviderFetched && networkStatus.isConnected()) {
            // Load any random order from the db and use it for the fetch.
            val order = orderStore.getOrdersForSite(selectedSite.get())[0]
            val payload = FetchOrderShipmentProvidersPayload(selectedSite.get(), order)
            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentProvidersAction(payload))
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderStatusOptionsChanged(event: OnOrderStatusOptionsChanged) {
        isRefreshingOrderStatusOptions = false

        if (event.isError) {
            WooLog.e(
                    WooLog.T.ORDERS,
                    "$TAG - Error fetching order status options from the api : ${event.error.message}")
            return
        }

        if (event.rowsAffected > 0) {
            orderView?.setOrderStatusOptions(getOrderStatusOptions())
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        when (event.causeOfChange) {
            // A child fragment made a change that requires a data refresh.
            UPDATE_ORDER_STATUS -> orderView?.invalidateListData()
            else -> {}
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderShipmentProviderChanged(event: OnOrderShipmentProvidersChanged) {
        if (event.isError) {
            WooLog.e(WooLog.T.ORDERS, "$TAG - Error fetching shipment tracking providers : ${event.error.message}")
        } else {
            AnalyticsTracker.track(Stat.ORDER_TRACKING_PROVIDERS_LOADED)
            isShipmentTrackingProviderFetched = true
        }
    }
}
