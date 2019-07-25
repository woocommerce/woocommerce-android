package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderListPresenter
import com.woocommerce.android.util.WooLog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import javax.inject.Inject

@ActivityScope
class OrderListPresenterNew @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus
) : OrderListContractNew.Presenter {
    companion object {
        private val TAG: String = OrderListPresenter::class.java.simpleName
    }

    private var orderView: OrderListContractNew.View? = null
    private var isRefreshingOrderStatusOptions = false

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

    override fun isOrderStatusOptionsRefreshing(): Boolean {
        return isRefreshingOrderStatusOptions
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
}
