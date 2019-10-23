package com.woocommerce.android.ui.orders.notes

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface AddOrderNoteContract {
    interface Presenter : BasePresenter<View> {
        fun hasBillingEmail(orderId: OrderIdentifier): Boolean
        fun pushOrderNote(orderId: OrderIdentifier, noteText: String, isCustomerNote: Boolean): Boolean
    }

    interface View : BaseView<Presenter> {
        fun getNoteText(): String
        fun confirmDiscard()
        fun showAddOrderNoteSnack()
        fun showAddOrderNoteErrorSnack()
        fun showOfflineSnack()
    }
}
