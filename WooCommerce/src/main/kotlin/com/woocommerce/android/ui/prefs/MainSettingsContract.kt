package com.woocommerce.android.ui.prefs

import android.content.Context
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface MainSettingsContract {
    interface Presenter : BasePresenter<View> {
        fun getUserDisplayName(): String
        fun getStoreDomainName(): String
        fun testNotification(context: Context)
    }

    interface View : BaseView<Presenter> {
        fun showDeviceAppNotificationSettings()
    }
}
