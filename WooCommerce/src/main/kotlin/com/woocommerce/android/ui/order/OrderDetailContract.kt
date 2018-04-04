package com.woocommerce.android.ui.order

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface OrderDetailContract {
    interface Presenter : BasePresenter<View> {
        /**
         * @param orderId The local order id
         */
        fun loadOrderDetail(orderId: Int)
    }

    interface View : BaseView<Presenter> {
        fun setLoadingIndicator(active: Boolean)
    }
}
