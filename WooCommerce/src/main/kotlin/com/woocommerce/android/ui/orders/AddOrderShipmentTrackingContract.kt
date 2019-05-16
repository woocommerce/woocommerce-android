package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface AddOrderShipmentTrackingContract {
    interface Presenter : BasePresenter<View>

    interface View : BaseView<BasePresenter<View>> {
        fun confirmDiscard()
        fun onActivityFinish()
        fun getDateShippedText(): String
        fun getProviderText(): String
    }
}
