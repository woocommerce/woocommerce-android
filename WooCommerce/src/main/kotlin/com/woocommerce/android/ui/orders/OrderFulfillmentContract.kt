package com.woocommerce.android.ui.orders

import android.content.Context
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrderFulfillmentContract {
    interface Presenter : BasePresenter<View> {
        var orderModel: WCOrderModel?
        fun loadOrderDetail(orderIdentifier: OrderIdentifier)
        fun markOrderComplete(context: Context)
    }

    interface View : BaseView<Presenter> {
        fun showOrderDetail(order: WCOrderModel)
        fun showNetworkConnectivityError()
        fun toggleCompleteButton(isEnabled: Boolean)
        fun orderFulfilled()
    }
}
