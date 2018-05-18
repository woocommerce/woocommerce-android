package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrderProductListContract {
    interface Presenter : BasePresenter<View> {
        fun loadOrderDetail(orderIdentifier: OrderIdentifier)
    }

    interface View : BaseView<Presenter> {
        fun showOrderProducts(order: WCOrderModel)
    }
}
