package com.woocommerce.android.ui.main

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import com.woocommerce.android.ui.base.TopLevelFragmentRouter
import org.wordpress.android.fluxc.model.notification.NotificationModel

interface MainContract {
    interface Presenter : BasePresenter<View> {
        fun userIsLoggedIn(): Boolean
        fun storeMagicLinkToken(token: String)
        fun getNotificationByRemoteNoteId(remoteNoteId: Long): NotificationModel?
    }

    interface View : BaseView<Presenter>, TopLevelFragmentRouter {
        fun notifyTokenUpdated()
        fun showLoginScreen()
        fun showLoginEpilogueScreen()
        fun updateSelectedSite()
        fun showSettingsScreen()
        fun showHelpAndSupport()
        fun updateOfflineStatusBar(isConnected: Boolean)
        fun hideBottomNav()
        fun showBottomNav()
        fun showNotificationBadge(show: Boolean)
        fun updateNotificationBadge()
    }
}
