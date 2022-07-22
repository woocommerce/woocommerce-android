package com.woocommerce.android.ui.prefs

import androidx.lifecycle.MutableLiveData
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
        fun onCtaClicked(source: String)
        fun onDismissClicked()
        fun onRemindLaterClicked(currentTimeInMillis: Long, source: String)
        fun onDontShowAgainClicked(source: String)
        fun onBannerAlertDismiss()
        fun canShowCardReaderUpsellBanner(currentTimeInMillis: Long, source: String): Boolean
        val shouldShowUpsellCardReaderDismissDialog: MutableLiveData<Boolean>
        val isEligibleForInPersonPayments: MutableLiveData<Boolean>
    }

    interface View : BaseView<Presenter> {
        fun showDeviceAppNotificationSettings()
        fun showLatestAnnouncementOption(announcement: FeatureAnnouncement)
        fun handleJetpackInstallOption(isJetpackCPSite: Boolean)
        fun dismissUpsellCardReaderBanner()
        fun dismissUpsellCardReaderBannerViaBack()
        fun dismissUpsellCardReaderBannerViaRemindLater()
        fun dismissUpsellCardReaderBannerViaDontShowAgain()
        fun openPurchaseCardReaderLink(url: String)
    }
}
