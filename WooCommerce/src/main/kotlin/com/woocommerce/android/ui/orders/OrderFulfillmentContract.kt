package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrderFulfillmentContract {
    interface Presenter : BasePresenter<View> {
        fun loadOrderDetail(orderIdentifier: OrderIdentifier)
    }

    interface View : BaseView<Presenter> {
        fun showOrderDetail(order: WCOrderModel?)
    }
}
