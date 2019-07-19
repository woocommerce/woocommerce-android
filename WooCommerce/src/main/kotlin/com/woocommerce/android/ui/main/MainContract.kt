package com.woocommerce.android.ui.main

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import com.woocommerce.android.ui.base.TopLevelFragmentRouter
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NotificationModel

interface MainContract {
    interface Presenter : BasePresenter<View> {
        fun userIsLoggedIn(): Boolean
        fun storeMagicLinkToken(token: String)
        fun getNotificationByRemoteNoteId(remoteNoteId: Long): NotificationModel?
        fun hasMultipleStores(): Boolean
        fun selectedSiteChanged(site: SiteModel)
        fun fetchUnfilledOrderCount()
    }

    interface View : BaseView<Presenter>, TopLevelFragmentRouter {
        fun notifyTokenUpdated()
        fun showLoginScreen()
        fun showSitePickerScreen()
        fun updateSelectedSite()
        fun showSettingsScreen()
        fun showHelpAndSupport()
        fun updateOfflineStatusBar(isConnected: Boolean)
        fun hideBottomNav()
        fun showBottomNav()
        fun hideNotificationBadge()
        fun showNotificationBadge()
        fun updateNotificationBadge()
        fun hideOrderBadge()
        fun showOrderBadge(count: Int)
        fun updateOrderBadge(hideCountUntilComplete: Boolean)
    }
}
