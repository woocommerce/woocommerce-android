package com.woocommerce.android.ui.login

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface LoginEpilogueContract {
    interface Presenter : BasePresenter<View> {
        // TODO
    }

    interface View : BaseView<Presenter> {
        fun updateAvatar()
    }
}
