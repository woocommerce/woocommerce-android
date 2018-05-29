package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import com.woocommerce.android.ui.base.ConnectionCheckView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrderFulfillmentContract {
    interface Presenter : BasePresenter<View> {
        var orderModel: WCOrderModel?
        fun loadOrderDetail(orderIdentifier: OrderIdentifier)
        fun markOrderComplete()
    }

    interface View : BaseView<Presenter>, ConnectionCheckView {
        fun showOrderDetail(order: WCOrderModel)
        fun toggleCompleteButton(isEnabled: Boolean)
        fun fulfillOrder()
    }
}
