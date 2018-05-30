package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import com.woocommerce.android.ui.base.ConnectionCheckView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrderDetailContract {
    interface Presenter : BasePresenter<View> {
        var orderModel: WCOrderModel?
        fun loadOrderDetail(orderIdentifier: OrderIdentifier, markComplete: Boolean)
        fun loadOrderNotes()
        fun doMarkOrderComplete()
    }

    interface View : BaseView<Presenter>, OrderActionListener, ConnectionCheckView {
        fun showOrderDetail(order: WCOrderModel?)
        fun showOrderNotes(notes: List<WCOrderNoteModel>)
        fun updateOrderNotes(notes: List<WCOrderNoteModel>)
        fun showUndoOrderCompleteSnackbar()
        fun markOrderCompleteSuccess()
        fun markOrderCompleteFailed()
    }
}
