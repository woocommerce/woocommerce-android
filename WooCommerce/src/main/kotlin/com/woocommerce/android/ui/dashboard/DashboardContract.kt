package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface DashboardContract {
    interface Presenter : BasePresenter<View>

    interface View : BaseView<Presenter> {
        var isActive: Boolean

        fun setLoadingIndicator(active: Boolean)
    }
}
