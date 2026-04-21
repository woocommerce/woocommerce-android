package com.woocommerce.android.ui.prefs

import com.woocommerce.android.model.FeatureAnnouncement
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface MainSettingsContract {
    interface Presenter : BasePresenter<View> {
        val isChaChingSoundEnabled: Boolean
        fun getUserDisplayName(): String
        fun getStoreDomainName(): String
        fun hasMultipleStores(): Boolean
        fun setupAnnouncementOption()
        fun setupEnablePushNotificationsOption()
        fun setupJetpackInstallOption()
        fun onNotificationsClicked()

        val isCloseAccountOptionVisible: Boolean
        val isThemePickerOptionVisible: Boolean
        val isPluginsSectionVisible: Boolean
        val wooPluginVersion: String
    }

    interface View : BaseView<Presenter> {
        fun showDeviceAppNotificationSettings()
        fun showNotificationsSettingsScreen()
        fun showLatestAnnouncementOption(announcement: FeatureAnnouncement)
        fun setEnablePushNotificationsOptionVisible(isVisible: Boolean)
        fun handleJetpackInstallOption(supportsJetpackInstallation: Boolean)
    }
}
