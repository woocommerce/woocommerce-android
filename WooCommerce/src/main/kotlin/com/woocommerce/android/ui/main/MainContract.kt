package com.woocommerce.android.ui.main

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface MainContract {
    interface Presenter : BasePresenter<View> {
        fun userIsLoggedIn(): Boolean
        fun storeMagicLinkToken(token: String)
    }

    interface View : BaseView<Presenter> {
        fun notifyTokenUpdated()
        fun showLoginScreen()
        fun showLoginEpilogueScreen()
        fun updateSelectedSite()
        fun showSettingsScreen()
        fun contactSupport()
    }
}
