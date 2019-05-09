package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface AddOrderShipmentTrackingContract {
    interface Presenter : BasePresenter<View> {
        fun getOrderByIdentifier(orderId: OrderIdentifier): WCOrderModel?
    }

    interface View : BaseView<Presenter> {
        fun getDateShippedText(): String
    }
}
