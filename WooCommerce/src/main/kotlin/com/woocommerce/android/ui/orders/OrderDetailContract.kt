package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrderDetailContract {
    interface Presenter : BasePresenter<View> {
        var orderModel: WCOrderModel?
        fun loadOrderDetail(orderIdentifier: OrderIdentifier)
        fun updateOrderStatus(status: String)
        fun loadOrderNotes()
    }

    interface View : BaseView<Presenter>, OrderActionListener {
        fun showOrderDetail(order: WCOrderModel?)
        fun showOrderNotes(notes: List<WCOrderNoteModel>)
        fun updateOrderNotes(notes: List<WCOrderNoteModel>)
        fun orderStatusUpdateSuccess(order: WCOrderModel)
        fun isNetworkConnected(): Boolean
        fun showNetworkErrorForNotes()
        fun showNetworkErrorForUpdateOrderStatus()
    }
}
