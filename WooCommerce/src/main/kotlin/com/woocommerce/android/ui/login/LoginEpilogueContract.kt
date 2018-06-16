package com.woocommerce.android.ui.login

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.SiteModel

interface LoginEpilogueContract {
    interface Presenter : BasePresenter<View> {
        fun getWooCommerceSites(): List<SiteModel>
    }

    interface View : BaseView<Presenter> {
        fun showUserInfo()
        fun showSiteList()
    }
}
