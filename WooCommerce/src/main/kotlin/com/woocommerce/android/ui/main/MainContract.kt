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
        fun fetchSitesAfterDowngrade()
        fun isUserEligible(): Boolean
        fun updateStatsWidgets()
        fun onPlanUpgraded()
        fun onPlanUpgradeDismissed()
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
        fun hideProgressDialog()
        fun showProgressDialog(@StringRes stringId: Int)
        fun showUserEligibilityErrorScreen()
        fun updateStatsWidgets()
        fun restart()
    }
}
