package com.woocommerce.android.ui.notifications

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface ReviewDetailContract {
    interface Presenter: BasePresenter<View>

    interface View : BaseView<Presenter> {
        fun showLoadingSkeleton()
    }
}
