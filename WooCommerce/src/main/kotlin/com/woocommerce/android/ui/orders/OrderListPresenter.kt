package com.woocommerce.android.ui.orders

import android.util.Log
import com.woocommerce.android.tools.SelectedSite
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject

class OrderListPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) : OrderListContract.Presenter {
    private var orderView: OrderListContract.View? = null

    override fun takeView(view: OrderListContract.View) {
        orderView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        orderView = null
        dispatcher.unregister(this)
    }

    override fun loadOrders() {
        orderView?.setLoadingIndicator(true)

        val payload = FetchOrdersPayload(selectedSite.get())
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.isError) {
            // TODO: Notify the user of the problem
            Log.e(this::class.java.simpleName, "Error fetching orders : ${event.error.message}")
            return
        }

        if (event.causeOfChange == FETCH_ORDERS) {
            val orders = orderStore.getOrdersForSite(selectedSite.get())
            if (orders.count() > 0) {
                orderView?.showOrders(orders)
            } else {
                orderView?.showNoOrders()
            }

            orderView?.setLoadingIndicator(false)
        }
    }

    override fun openOrderDetail(order: WCOrderModel) {
        orderView?.openOrderDetail(order, false)
    }
}
