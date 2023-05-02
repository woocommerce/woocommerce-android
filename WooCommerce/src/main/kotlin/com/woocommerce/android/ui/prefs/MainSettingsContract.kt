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
        fun setupJetpackInstallOption()
        fun setupApplicationPasswordsSettings()

        fun setupOnboardingListVisibilitySetting()

        val isDomainOptionVisible: Boolean
    }

    interface View : BaseView<Presenter> {
        fun showDeviceAppNotificationSettings()
        fun showLatestAnnouncementOption(announcement: FeatureAnnouncement)
        fun handleJetpackInstallOption(supportsJetpackInstallation: Boolean)
        fun handleApplicationPasswordsSettings()
        fun handleStoreSetupListSetting(enabled: Boolean, onToggleChange: (Boolean) -> Unit)
    }
}
