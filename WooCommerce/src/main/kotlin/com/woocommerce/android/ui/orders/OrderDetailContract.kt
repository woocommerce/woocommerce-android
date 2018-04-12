package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel

interface OrderDetailContract {
    interface Presenter : BasePresenter<View> {
        /**
         * Fetch an order from the local database
         * @param orderId The local order id
         */
        fun loadOrderDetail(orderId: Int)

        /**
         * Fetch a fresh copy of an order from the API
         * @param remoteOrderId The server order id
         */
        fun refreshOrderDetail(remoteOrderId: Long)
    }

    interface View : BaseView<Presenter>, OrderActionListener {
        fun setLoadingIndicator(active: Boolean)
        fun showOrderDetail(order: WCOrderModel?)
    }
}
