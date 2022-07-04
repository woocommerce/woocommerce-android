package com.woocommerce.android.ui.main

import androidx.annotation.StringRes
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.SiteModel

interface MainContract {
    interface Presenter : BasePresenter<View> {
        fun userIsLoggedIn(): Boolean
        fun storeMagicLinkToken(token: String)
        fun hasMultipleStores(): Boolean
        fun selectedSiteChanged(site: SiteModel)
        fun fetchUnfilledOrderCount()
        fun fetchSitesAfterDowngrade()
        fun isUserEligible(): Boolean
    }

    interface View : BaseView<Presenter> {
        fun notifyTokenUpdated()
        fun showLoginScreen()
        fun updateSelectedSite()
        fun updateOfflineStatusBar(isConnected: Boolean)
        fun hideBottomNav()
        fun showBottomNav()
        fun hideOrderBadge()
        fun showOrderBadge(count: Int)
        fun updateOrderBadge(hideCountUntilComplete: Boolean)
        fun hideProgressDialog()
        fun showProgressDialog(@StringRes stringId: Int)
        fun showUserEligibilityErrorScreen()
    }
}
