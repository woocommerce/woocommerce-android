package com.woocommerce.android.ui.login

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface MagicLinkInterceptContract {
    interface Presenter : BasePresenter<View> {
        fun userIsLoggedIn(): Boolean
        fun storeMagicLinkToken(token: String)
    }

    interface View : BaseView<Presenter> {
        fun notifyMagicLinkTokenUpdated()
        fun hideProgressDialog()
        fun showProgressDialog()
        fun showLoginScreen()
        fun showSitePickerScreen()
    }
}
