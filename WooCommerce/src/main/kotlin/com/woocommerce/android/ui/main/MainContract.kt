package com.woocommerce.android.ui.main

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.SiteModel

interface MainContract {
    interface Presenter : BasePresenter<View> {
        fun userIsLoggedIn(): Boolean
        fun getWooCommerceSites(): List<SiteModel>
        fun storeMagicLinkToken(token: String)
        fun logout()
    }

    interface View : BaseView<Presenter> {
        fun notifyTokenUpdated()
        fun showLoginScreen()
        fun updateSelectedSite()
    }
}
