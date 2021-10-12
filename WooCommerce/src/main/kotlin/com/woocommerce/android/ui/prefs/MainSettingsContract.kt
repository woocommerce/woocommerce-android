package com.woocommerce.android.ui.prefs

import com.woocommerce.android.model.FeatureAnnouncement
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface MainSettingsContract {
    interface Presenter : BasePresenter<View> {
        fun getUserDisplayName(): String
        fun getStoreDomainName(): String
        fun hasMultipleStores(): Boolean
        fun setupAnnouncementOption()
    }

    interface View : BaseView<Presenter> {
        fun showDeviceAppNotificationSettings()
        fun showLatestAnnouncementOption(announcement: FeatureAnnouncement)
    }
}
