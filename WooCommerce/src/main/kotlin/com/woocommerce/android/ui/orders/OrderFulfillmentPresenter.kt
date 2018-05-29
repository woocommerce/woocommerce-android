package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.orders.OrderFulfillmentContract.View
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderFulfillmentPresenter @Inject constructor(
    private val orderStore: WCOrderStore
) : OrderFulfillmentContract.Presenter {
    override var orderModel: WCOrderModel? = null
    private var orderView: OrderFulfillmentContract.View? = null

    override fun takeView(view: View) {
        orderView = view
    }

    override fun dropView() {
        orderView = null
    }

    override fun loadOrderDetail(orderIdentifier: OrderIdentifier) {
        orderView?.let { view ->
            orderModel = orderStore.getOrderByIdentifier(orderIdentifier)?.also {
                view.showOrderDetail(it)
            }
        }
    }

    override fun markOrderComplete() {
        orderView?.let {
            if (it.isNetworkConnected()) {
                // Start the process of fulfilling the order
                it.fulfillOrder()
            } else {
                // Notify user unable to mark order complete due to no connectivity
                it.showNetworkConnectivityError()
            }
        }
    }
}
