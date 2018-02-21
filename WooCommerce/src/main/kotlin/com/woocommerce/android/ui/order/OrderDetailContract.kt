package com.woocommerce.android.ui.order

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface OrderDetailContract {
    interface Presenter : BasePresenter<View> {
        fun loadOrderDetail(orderNum: Long)
    }

    interface View : BaseView<Presenter> {
        fun setLoadingIndicator(active: Boolean)
    }
}
