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
        fun canShowCardReaderUpsellBanner(currentTimeInMillis: Long, source: String): Boolean
        val isEligibleForInPersonPayments: MutableLiveData<Boolean>
    }

    interface View : BaseView<Presenter> {
        fun showDeviceAppNotificationSettings()
        fun showLatestAnnouncementOption(announcement: FeatureAnnouncement)
        fun handleJetpackInstallOption(isJetpackCPSite: Boolean)
        fun openPurchaseCardReaderLink(url: String)
    }
}
