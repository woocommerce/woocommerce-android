package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel

interface OrderDetailContract {
    interface Presenter : BasePresenter<View> {
        fun loadOrderDetail(orderId: Int)
    }

    interface View : BaseView<Presenter>, OrderActionListener {
        fun setLoadingIndicator(active: Boolean)
        fun showOrderDetail(order: WCOrderModel?)
    }
}
