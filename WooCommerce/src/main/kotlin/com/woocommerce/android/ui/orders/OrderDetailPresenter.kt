package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderDetailPresenter @Inject constructor(private val orderStore: WCOrderStore) : OrderDetailContract.Presenter {
    private var orderView: OrderDetailContract.View? = null

    override fun takeView(view: OrderDetailContract.View) {
        orderView = view
    }

    override fun dropView() {
        orderView = null
    }

    override fun loadOrderDetail(orderId: Int) {
        if (orderId != 0) {
            orderView?.let {
                val order = orderStore.getOrderByLocalOrderId(orderId)
                orderView?.showOrderDetail(order)
            }
        }
    }

    override fun refreshOrderDetail(remoteOrderId: Long) {
        // TODO - fetch a fresh copy of this order from the server.
        // for now just cancel the loading indicator
        orderView?.setLoadingIndicator(false)
    }
}
