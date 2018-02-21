package com.woocommerce.android.ui.orderlist

import com.woocommerce.android.ui.order.OrderDetailFragment
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderListPresenter @Inject constructor(private var orderStore: WCOrderStore) : OrderListContract.Presenter {
    private var orderView: OrderListContract.View? = null

    override fun takeView(view: OrderListContract.View) {
        orderView = view
        loadOrders()
    }

    override fun dropView() {
        orderView = null
    }

    override fun loadOrders() {
        orderView?.setLoadingIndicator(true)

        val orders = orderStore.getNewOrders()
        if (orders.count() > 0) {
            orderView?.showOrders(orders)
        } else {
            orderView?.showNoOrders()
        }

        orderView?.setLoadingIndicator(false)
    }

    override fun openOrderDetail(orderId: Long) {
        val frag = OrderDetailFragment.newInstance(orderId)
        orderView?.loadChildFragment(frag)
    }
}
