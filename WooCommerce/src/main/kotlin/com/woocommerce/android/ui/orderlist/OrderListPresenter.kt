package com.woocommerce.android.ui.orderlist

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject

class OrderListPresenter @Inject constructor(private val dispatcher: Dispatcher,
                                             private val orderStore: WCOrderStore
) : OrderListContract.Presenter {
    private var orderView: OrderListContract.View? = null

    override fun takeView(view: OrderListContract.View) {
        orderView = view
        dispatcher.register(this)
        loadOrders()
    }

    override fun dropView() {
        orderView = null
        dispatcher.unregister(this)
    }

    override fun loadOrders() {
        orderView?.setLoadingIndicator(true)

        orderView?.getSelectedSite()?.let {
            val payload = FetchOrdersPayload(it)
            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.isError) {
            // TODO: Notify the user of the problem
            return
        }

        // TODO: Temporary, we should be able to guarantee this Presenter is initialized with a non-null SiteModel
        val selectedSite = orderView?.getSelectedSite() ?: return

        val orders = orderStore.getOrdersForSite(selectedSite)
        if (orders.count() > 0) {
            orderView?.showOrders(orders)
        } else {
            orderView?.showNoOrders()
        }

        orderView?.setLoadingIndicator(false)
    }

    override fun openOrderDetail(orderId: Long) {
        orderView?.openOrderDetail(orderId)
    }
}
