package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel

interface OrderDetailContract {
    interface Presenter : BasePresenter<View> {
        /**
         * @param orderId The local order id
         */
        fun loadOrderDetail(orderId: Int)
    }

    interface View : BaseView<Presenter>, OrderActionListener {
        fun showOrderDetail(order: WCOrderModel?, notes: List<WCOrderNoteModel>)
        fun updateOrderNotes(notes: List<WCOrderNoteModel>)
    }
}
