package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface AddOrderNoteContract {
    interface Presenter : BasePresenter<View> {
        fun pushOrderNote(orderId: OrderIdentifier, noteText: String, isCustomerNote: Boolean)
        fun hasBillingEmail(orderId: OrderIdentifier): Boolean
    }

    interface View : BaseView<Presenter> {
        fun doBeforeAddNote()
        fun doAfterAddNote(didSucceed: Boolean)
        fun showNullOrderError()
    }
}
