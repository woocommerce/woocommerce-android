package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrderDetailAddNoteContract {
    interface Presenter : BasePresenter<View> {
        fun pushOrderNote(orderId: OrderIdentifier, noteText: String, isCustomerNote: Boolean)
    }

    interface View : BaseView<Presenter> {
        fun doBeforeAddNote()
        fun doAfterAddNote(didSucced: Boolean)
        fun showNullOrderError()
    }
}
