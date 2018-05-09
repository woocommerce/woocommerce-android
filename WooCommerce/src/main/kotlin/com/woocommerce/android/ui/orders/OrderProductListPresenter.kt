package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderProductListPresenter @Inject constructor(
    private val orderStore: WCOrderStore
) : OrderProductListContract.Presenter {
    private var productView: OrderProductListContract.View? = null

    override fun takeView(view: OrderProductListContract.View) {
        productView = view
    }

    override fun dropView() {
        productView = null
    }

    override fun loadOrderDetail(orderId: Int) {
        if (orderId != 0) {
            productView?.let { view ->
                val orderModel = orderStore.getOrderByLocalOrderId(orderId)
                orderModel?.let {
                    view.showOrderProducts(it)
                }
            }
        }
    }
}
