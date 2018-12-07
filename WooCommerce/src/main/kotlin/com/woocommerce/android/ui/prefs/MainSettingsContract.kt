package com.woocommerce.android.ui.prefs

import android.content.Context
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface MainSettingsContract {
    interface Presenter : BasePresenter<View> {
        fun getUserDisplayName(): String
        fun getStoreDomainName(): String
        fun testNotif(context: Context) // TODO: remove before merging
    }

    interface View : BaseView<Presenter> {
        // TODO: empty for now but will be added to as more settings are added
    }
}
