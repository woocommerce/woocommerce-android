package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.orders.OrderFulfillmentContract.View
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderFulfillmentPresenter @Inject constructor(
    private val orderStore: WCOrderStore
) : OrderFulfillmentContract.Presenter {
    private var orderView: OrderFulfillmentContract.View? = null

    override fun takeView(view: View) {
        orderView = view
    }

    override fun dropView() {
        orderView = null
    }

    override fun loadOrderDetail(orderId: Int) {
        if (orderId != 0) {
            orderView?.let { view ->
                val orderModel = orderStore.getOrderByLocalOrderId(orderId)
                orderModel?.let {
                    view.showOrderDetail(it)
                }
            }
        }
    }
}
