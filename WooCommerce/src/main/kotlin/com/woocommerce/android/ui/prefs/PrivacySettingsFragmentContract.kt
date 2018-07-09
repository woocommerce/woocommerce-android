package com.woocommerce.android.ui.prefs

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface PrivacySettingsFragmentContract {
    interface Presenter : BasePresenter<View> {
        fun updateUsagePref(allowUsageTracking: Boolean)
    }

    interface View : BaseView<Presenter> {
        fun showCookiePolicy()
        fun showPrivacyPolicy()
    }
}
