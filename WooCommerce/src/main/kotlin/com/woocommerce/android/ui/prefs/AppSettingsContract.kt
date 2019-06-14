package com.woocommerce.android.ui.prefs

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface AppSettingsContract {
    interface Presenter : BasePresenter<View> {
        fun logout()
        fun userIsLoggedIn(): Boolean
        fun getAccountDisplayName(): String
    }

    interface View : BaseView<Presenter> {
        fun close()
        fun finishLogout()
        fun confirmLogout()
        fun clearNotificationPreferences()
    }
}
