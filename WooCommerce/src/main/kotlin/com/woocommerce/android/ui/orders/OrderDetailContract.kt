package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrderDetailContract {
    interface Presenter : BasePresenter<View> {
        var orderModel: WCOrderModel?
        var isUsingCachedNotes: Boolean
        fun loadOrderDetail(orderIdentifier: OrderIdentifier, markComplete: Boolean)
        fun loadOrderNotes()
        fun doChangeOrderStatus(newStatus: String)
        fun pushOrderNote(noteText: String, isCustomerNote: Boolean)
    }

    interface View : BaseView<Presenter>, OrderActionListener {
        fun showOrderDetail(order: WCOrderModel?)
        fun showOrderNotes(notes: List<WCOrderNoteModel>)
        fun showAddOrderNoteScreen()
        fun updateOrderNotes(notes: List<WCOrderNoteModel>)
        fun updateOrderStatus(newStatus: String)
        fun showChangeOrderStatusSnackbar(newStatus: String)
        fun showNotesErrorSnack()
        fun showAddOrderNoteSnack()
        fun showAddOrderNoteErrorSnack()
        fun showOrderStatusChangedError()
        fun markOrderStatusChangedSuccess()
        fun markOrderStatusChangedFailed()
    }
}
