package com.woocommerce.android.ui.order

import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderDetailPresenter @Inject constructor(private var orderStore: WCOrderStore) : OrderDetailContract.Presenter {
    private var orderView: OrderDetailContract.View? = null

    override fun takeView(view: OrderDetailContract.View) {
        orderView = view
    }

    override fun dropView() {
        orderView = null
    }

    override fun loadOrderDetail(orderId: Int) {
        if (orderView != null) {
            orderView?.setLoadingIndicator(true)

            // todo - fetch the order detail

            orderView?.setLoadingIndicator(false)
        }
    }
}
