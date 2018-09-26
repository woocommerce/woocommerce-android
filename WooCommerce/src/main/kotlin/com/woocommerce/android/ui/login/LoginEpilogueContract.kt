package com.woocommerce.android.ui.login

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.SiteModel

interface LoginEpilogueContract {
    interface Presenter : BasePresenter<View> {
        fun getWooCommerceSites(): List<SiteModel>
        fun getSiteBySiteId(siteId: Long): SiteModel?
        fun getUserAvatarUrl(): String?
        fun getUserName(): String?
        fun getUserDisplayName(): String?
        fun logout()
        fun userIsLoggedIn(): Boolean
    }

    interface View : BaseView<Presenter> {
        fun showUserInfo()
        fun showStoreList()
        fun cancel()
    }
}
